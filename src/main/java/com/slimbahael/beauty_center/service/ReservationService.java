package com.slimbahael.beauty_center.service;

import com.slimbahael.beauty_center.dto.*;
import com.slimbahael.beauty_center.exception.BadRequestException;
import com.slimbahael.beauty_center.exception.ResourceNotFoundException;
import com.slimbahael.beauty_center.model.Reservation;
import com.slimbahael.beauty_center.model.Service;
import com.slimbahael.beauty_center.model.ServiceAddon;
import com.slimbahael.beauty_center.model.User;
import com.slimbahael.beauty_center.repository.ReservationRepository;
import com.slimbahael.beauty_center.repository.ServiceAddonRepository;
import com.slimbahael.beauty_center.repository.ServiceRepository;
import com.slimbahael.beauty_center.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceAddonRepository serviceAddonRepository;
    private final SmsService smsService;
    private final EmailService emailService;

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    public List<ReservationResponse> getAllReservations() {
        return reservationRepository.findAll()
                .stream()
                .map(this::mapReservationToResponse)
                .collect(Collectors.toList());
    }

    public List<ReservationResponse> getReservationsByCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return reservationRepository.findByCustomerId(customer.getId())
                .stream()
                .map(this::mapReservationToResponse)
                .sorted(Comparator.comparing(ReservationResponse::getReservationDate).reversed())
                .collect(Collectors.toList());
    }

    public List<ReservationResponse> getReservationsByStaff() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User staff = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return reservationRepository.findByStaffId(staff.getId())
                .stream()
                .map(this::mapReservationToResponse)
                .sorted(Comparator.comparing(ReservationResponse::getReservationDate))
                .collect(Collectors.toList());
    }

    public ReservationResponse getReservationById(String id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));

        // Check if user is authorized to view this reservation
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Admin can see all reservations, staff can see their assigned ones,
        // customers can see only their own reservations
        if (user.getRole().equals("ADMIN") ||
                user.getRole().equals("STAFF") && user.getId().equals(reservation.getStaffId()) ||
                user.getId().equals(reservation.getCustomerId())) {
            return mapReservationToResponse(reservation);
        } else {
            throw new BadRequestException("You are not authorized to view this reservation");
        }
    }

    public ReservationResponse createReservation(CreateReservationRequest request) {
        // Get authenticated user (customer)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate service exists
        Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        // Validate staff exists and is assigned to this service
        User staff = userRepository.findById(request.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        if (staff.getRole() == null || !staff.getRole().equals("STAFF")) {
            throw new BadRequestException("Selected user is not a staff member");
        }

        if (service.getAssignedStaffIds() == null || !service.getAssignedStaffIds().contains(staff.getId())) {
            throw new BadRequestException("Selected staff is not assigned to this service");
        }

        // Validate time slot
        if (!request.getTimeSlot().equals("MORNING") && !request.getTimeSlot().equals("EVENING")) {
            throw new BadRequestException("Time slot must be MORNING or EVENING");
        }

        // Check if service is available in the selected time slot
        if (request.getTimeSlot().equals("MORNING")) {
            if (staff.getMorningShift() == null || !staff.getMorningShift().equals("YES")) {
                throw new BadRequestException("Staff is not available in the morning slot");
            }
        } else {
            if (staff.getEveningShift() == null || !staff.getEveningShift().equals("YES")) {
                throw new BadRequestException("Staff is not available in the evening slot");
            }
        }


        // Check if staff works on the selected day of week
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(request.getReservationDate());
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        String[] daysOfWeek = {"", "SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"};
        String dayOfWeekString = daysOfWeek[dayOfWeek];

        if (staff.getWorkDays() == null || !staff.getWorkDays().contains(dayOfWeekString)) {
            throw new BadRequestException("Staff does not work on the selected day");
        }

        // Check if staff is already booked in the selected time slot
        List<Reservation> existingReservations = reservationRepository
                .findByStaffIdAndReservationDateAndTimeSlot(
                        request.getStaffId(),
                        request.getReservationDate(),
                        request.getTimeSlot());

        if (!existingReservations.isEmpty()) {
            throw new BadRequestException("Staff is already booked for the selected time slot");
        }

        // Calculate total price
        BigDecimal totalAmount = service.getPrice();

        // Add addons price if any
        List<String> addonIds = request.getAddonIds();
        if (addonIds != null && !addonIds.isEmpty()) {
            for (String addonId : addonIds) {
                ServiceAddon addon = serviceAddonRepository.findById(addonId)
                        .orElseThrow(() -> new ResourceNotFoundException("Service addon not found"));

                // Validate addon is compatible with the selected service
                if (addon.getCompatibleServiceIds() == null ||
                        !addon.getCompatibleServiceIds().contains(service.getId())) {
                    throw new BadRequestException("Selected addon is not compatible with the service");
                }

                totalAmount = totalAmount.add(addon.getPrice());
            }
        }

        // Create reservation
        Reservation reservation = Reservation.builder()
                .customerId(customer.getId())
                .staffId(request.getStaffId())
                .reservationDate(request.getReservationDate())
                .timeSlot(request.getTimeSlot())
                .serviceId(request.getServiceId())
                .addonIds(request.getAddonIds())
                .status("CONFIRMED")
                .totalAmount(totalAmount)
                .createdAt(new Date())
                .updatedAt(new Date())
                .notes(request.getNotes())
                .smsReminderSent(false)
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);

        // Send SMS notification to staff
        try {
            String staffPhoneNumber = staff.getPhoneNumber();
            if (staffPhoneNumber != null && !staffPhoneNumber.isEmpty()) {
                String message = String.format(
                    "New reservation for %s on %s at %s. Customer: %s",
                    service.getName(),
                    new SimpleDateFormat("MM/dd/yyyy").format(request.getReservationDate()),
                    request.getTimeSlot(),
                    customer.getFirstName() + " " + customer.getLastName()
                );
                smsService.sendSms(staffPhoneNumber, message);
            }
        } catch (Exception e) {
            log.error("Failed to send SMS notification for reservation {}: {}", savedReservation.getId(), e.getMessage());
        }

        // Send reservation confirmation email to customer
        try {
            emailService.sendReservationConfirmationEmail(
                customer.getEmail(),
                mapReservationToResponse(savedReservation)
            );
        } catch (Exception e) {
            log.error("Failed to send reservation confirmation email for reservation {}: {}", savedReservation.getId(), e.getMessage());
        }

        return mapReservationToResponse(savedReservation);
    }

    public ReservationResponse updateReservationStatus(String id, String status) {
        // Validate status
        if (!Arrays.asList("CONFIRMED", "COMPLETED", "CANCELLED").contains(status)) {
            throw new BadRequestException("Status must be CONFIRMED, COMPLETED or CANCELLED");
        }

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));

        // Check authorization
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Only admin, assigned staff, or the customer can update status
        if (!user.getRole().equals("ADMIN") &&
                !(user.getRole().equals("STAFF") && user.getId().equals(reservation.getStaffId())) &&
                !user.getId().equals(reservation.getCustomerId())) {
            throw new BadRequestException("You are not authorized to update this reservation");
        }

        // Customers can only cancel
        if (user.getRole().equals("CUSTOMER") && !status.equals("CANCELLED")) {
            throw new BadRequestException("Customers can only cancel reservations");
        }

        reservation.setStatus(status);
        reservation.setUpdatedAt(new Date());

        Reservation updatedReservation = reservationRepository.save(reservation);

        // Send notification when reservation is cancelled or completed
        if (status.equals("CANCELLED") || status.equals("COMPLETED")) {
            User customer = userRepository.findById(reservation.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

            User staff = userRepository.findById(reservation.getStaffId())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

            Service service = serviceRepository.findById(reservation.getServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

            // Notify staff about cancellation
            if (status.equals("CANCELLED") && staff.getPhoneNumber() != null && !staff.getPhoneNumber().isEmpty()) {
                String message = String.format(
                        "Booking cancelled: %s %s has cancelled %s on %s (%s)",
                        customer.getFirstName(),
                        customer.getLastName(),
                        service.getName(),
                        new java.text.SimpleDateFormat("yyyy-MM-dd").format(reservation.getReservationDate()),
                        reservation.getTimeSlot()
                );
                smsService.sendSms(staff.getPhoneNumber(), message);
            }

            // Notify customer about completion
            if (status.equals("COMPLETED") && customer.getPhoneNumber() != null && !customer.getPhoneNumber().isEmpty()) {
                String message = String.format(
                        "Thank you for visiting us! Your %s service has been completed. We hope to see you again soon!",
                        service.getName()
                );
                smsService.sendSms(customer.getPhoneNumber(), message);
            }
        }

        return mapReservationToResponse(updatedReservation);
    }

    public AvailabilityResponse checkAvailability(AvailabilityRequest request) {
        // Validate service
        Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        // Get assigned staff for the service
        List<String> assignedStaffIds = service.getAssignedStaffIds();
        if (assignedStaffIds == null || assignedStaffIds.isEmpty()) {
            throw new BadRequestException("No staff assigned to this service");
        }

        // Get day of week for the requested date
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(request.getDate());
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        String[] daysOfWeek = {"", "SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"};
        String dayOfWeekString = daysOfWeek[dayOfWeek];

        // Check which staff are available on this day
        List<User> assignedStaff = userRepository.findAllById(assignedStaffIds);
        List<User> availableStaff = assignedStaff.stream()
                .filter(staff -> staff.getWorkDays() != null && staff.getWorkDays().contains(dayOfWeekString))
                .collect(Collectors.toList());

        if (availableStaff.isEmpty()) {
            return AvailabilityResponse.builder()
                    .date(request.getDate())
                    .morningAvailable(false)
                    .eveningAvailable(false)
                    .availableStaff(new ArrayList<>())
                    .build();
        }

        // Check existing reservations for this date
        List<Reservation> existingReservations = reservationRepository.findByReservationDate(request.getDate());

        // Prepare staff availability map for morning
        Map<String, Boolean> staffMorningAvailability = new HashMap<>();
        for (User staff : availableStaff) {
            boolean isAvailableMorning = service.isAvailableMorning() &&
                    staff.getMorningShift() != null && staff.getMorningShift().equals("YES");
            staffMorningAvailability.put(staff.getId(), isAvailableMorning);
        }

        // Prepare staff availability map for evening
        Map<String, Boolean> staffEveningAvailability = new HashMap<>();
        for (User staff : availableStaff) {
            boolean isAvailableEvening = service.isAvailableEvening() &&
                    staff.getEveningShift() != null && staff.getEveningShift().equals("YES");
            staffEveningAvailability.put(staff.getId(), isAvailableEvening);
        }

        // Update availability based on existing reservations
        for (Reservation existingReservation : existingReservations) {
            if (existingReservation.getTimeSlot().equals("MORNING")) {
                staffMorningAvailability.put(existingReservation.getStaffId(), false);
            } else if (existingReservation.getTimeSlot().equals("EVENING")) {
                staffEveningAvailability.put(existingReservation.getStaffId(), false);
            }
        }

        // Build staff availability response list
        List<AvailabilityResponse.StaffAvailability> staffAvailabilities = new ArrayList<>();
        for (User staff : availableStaff) {
            boolean availableMorning = staffMorningAvailability.getOrDefault(staff.getId(), false);
            boolean availableEvening = staffEveningAvailability.getOrDefault(staff.getId(), false);

            if (availableMorning || availableEvening) {
                staffAvailabilities.add(AvailabilityResponse.StaffAvailability.builder()
                        .staffId(staff.getId())
                        .staffName(staff.getFirstName() + " " + staff.getLastName())
                        .staffImage(staff.getProfileImage())
                        .availableMorning(availableMorning)
                        .availableEvening(availableEvening)
                        .build());
            }
        }

        boolean morningAvailable = staffAvailabilities.stream().anyMatch(AvailabilityResponse.StaffAvailability::isAvailableMorning);
        boolean eveningAvailable = staffAvailabilities.stream().anyMatch(AvailabilityResponse.StaffAvailability::isAvailableEvening);

        return AvailabilityResponse.builder()
                .date(request.getDate())
                .morningAvailable(morningAvailable)
                .eveningAvailable(eveningAvailable)
                .availableStaff(staffAvailabilities)
                .build();
    }

    // Helper method to map Reservation entity to ReservationResponse DTO
    private ReservationResponse mapReservationToResponse(Reservation reservation) {
        // Get customer info
        User customer = userRepository.findById(reservation.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        // Get staff info
        User staff = userRepository.findById(reservation.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        // Get service info
        Service service = serviceRepository.findById(reservation.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        // Get addon info if any
        List<ReservationResponse.ServiceAddonInfo> addonInfoList = new ArrayList<>();
        if (reservation.getAddonIds() != null && !reservation.getAddonIds().isEmpty()) {
            List<ServiceAddon> addons = serviceAddonRepository.findAllById(reservation.getAddonIds());
            addonInfoList = addons.stream()
                    .map(addon -> ReservationResponse.ServiceAddonInfo.builder()
                            .id(addon.getId())
                            .name(addon.getName())
                            .price(addon.getPrice())
                            .build())
                    .collect(Collectors.toList());
        }

        return ReservationResponse.builder()
                .id(reservation.getId())
                .customerId(customer.getId())
                .customerName(customer.getFirstName() + " " + customer.getLastName())
                .staffId(staff.getId())
                .staffName(staff.getFirstName() + " " + staff.getLastName())
                .reservationDate(reservation.getReservationDate())
                .timeSlot(reservation.getTimeSlot())
                .serviceId(service.getId())
                .serviceName(service.getName())
                .addons(addonInfoList)
                .status(reservation.getStatus())
                .totalAmount(reservation.getTotalAmount())
                .createdAt(reservation.getCreatedAt())
                .notes(reservation.getNotes())
                .smsReminderSent(reservation.isSmsReminderSent())
                .build();
    }
}