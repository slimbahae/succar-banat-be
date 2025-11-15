package com.slimbahael.beauty_center.service;

import com.slimbahael.beauty_center.dto.CreateReservationRequest;
import com.slimbahael.beauty_center.exception.BadRequestException;
import com.slimbahael.beauty_center.model.Reservation;
import com.slimbahael.beauty_center.model.Service;
import com.slimbahael.beauty_center.model.User;
import com.slimbahael.beauty_center.repository.ReservationRepository;
import com.slimbahael.beauty_center.repository.ServiceAddonRepository;
import com.slimbahael.beauty_center.repository.ServiceRepository;
import com.slimbahael.beauty_center.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private ServiceAddonRepository serviceAddonRepository;
    @Mock
    private SmsService smsService;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private ReservationService reservationService;

    private User customer;
    private User staff;
    private Service service;
    private Date reservationDate;

    @BeforeEach
    void setup() {
        SecurityContextHolder.clearContext();
        customer = User.builder()
                .id("cust-1")
                .email("customer@example.com")
                .firstName("Jane")
                .lastName("Doe")
                .role("CUSTOMER")
                .build();

        staff = User.builder()
                .id("staff-1")
                .email("staff@example.com")
                .firstName("Alice")
                .lastName("Smith")
                .role("STAFF")
                .workDays(List.of("MONDAY"))
                .morningShift("YES")
                .eveningShift("YES")
                .phoneNumber("+15551234567")
                .build();

        service = Service.builder()
                .id("svc-1")
                .name("Facial Treatment")
                .price(new BigDecimal("80.00"))
                .assignedStaffIds(List.of("staff-1"))
                .availableMorning(true)
                .availableEvening(true)
                .build();

        Calendar calendar = Calendar.getInstance();
        calendar.set(2025, Calendar.JANUARY, 6, 9, 0, 0); // Monday
        calendar.set(Calendar.MILLISECOND, 0);
        reservationDate = calendar.getTime();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createReservationPersistsEntityAndNotifiesParties() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setStaffId("staff-1");
        request.setServiceId("svc-1");
        request.setTimeSlot("MORNING");
        request.setReservationDate(reservationDate);
        request.setAddonIds(Collections.emptyList());
        request.setNotes("Please prepare aromatherapy");

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(customer.getEmail(), null));

        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(serviceRepository.findById("svc-1")).thenReturn(Optional.of(service));
        when(userRepository.findById("staff-1")).thenReturn(Optional.of(staff));
        when(reservationRepository.findByStaffIdAndReservationDateAndTimeSlot(
                "staff-1", reservationDate, "MORNING")).thenReturn(Collections.emptyList());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation saved = invocation.getArgument(0);
            saved.setId("resv-1");
            return saved;
        });
        // mapReservationToResponse re-fetches users/services
        when(userRepository.findById("cust-1")).thenReturn(Optional.of(customer));
        when(serviceRepository.findById("svc-1")).thenReturn(Optional.of(service));

        var response = reservationService.createReservation(request);

        ArgumentCaptor<Reservation> reservationCaptor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(reservationCaptor.capture());
        Reservation saved = reservationCaptor.getValue();
        assertThat(saved.getCustomerId()).isEqualTo("cust-1");
        assertThat(saved.getStaffId()).isEqualTo("staff-1");
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("80.00");
        assertThat(saved.getStatus()).isEqualTo("CONFIRMED");

        assertThat(response.getId()).isEqualTo("resv-1");
        assertThat(response.getServiceName()).isEqualTo("Facial Treatment");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("80.00");

        verify(smsService).sendSms(eq(staff.getPhoneNumber()), any(String.class));
        verify(emailService).sendReservationConfirmationEmail(customer.getEmail(), response);
    }

    @Test
    void createReservationFailsWhenStaffNotAssignedToService() {
        service.setAssignedStaffIds(List.of("other-staff"));

        CreateReservationRequest request = new CreateReservationRequest();
        request.setStaffId("staff-1");
        request.setServiceId("svc-1");
        request.setTimeSlot("MORNING");
        request.setReservationDate(reservationDate);

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(customer.getEmail(), null));

        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(serviceRepository.findById("svc-1")).thenReturn(Optional.of(service));
        when(userRepository.findById("staff-1")).thenReturn(Optional.of(staff));

        assertThrows(BadRequestException.class, () -> reservationService.createReservation(request));

        verify(reservationRepository, never()).save(any());
        verify(smsService, never()).sendSms(any(), any());
        verify(emailService, never()).sendReservationConfirmationEmail(any(), any());
    }
}
