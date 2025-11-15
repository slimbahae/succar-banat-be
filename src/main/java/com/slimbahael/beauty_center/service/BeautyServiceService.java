package com.slimbahael.beauty_center.service;

import com.slimbahael.beauty_center.dto.ServiceRequest;
import com.slimbahael.beauty_center.dto.ServiceResponse;
import com.slimbahael.beauty_center.exception.ResourceNotFoundException;
import com.slimbahael.beauty_center.model.Service;
import com.slimbahael.beauty_center.repository.ServiceRepository;
import com.slimbahael.beauty_center.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class BeautyServiceService {

    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;

    public List<ServiceResponse> getAllServices() {
        return serviceRepository.findAll()
                .stream()
                .map(this::mapServiceToResponse)
                .collect(Collectors.toList());
    }

    public List<ServiceResponse> getActiveServices() {
        return serviceRepository.findByActiveIsTrue()
                .stream()
                .map(this::mapServiceToResponse)
                .collect(Collectors.toList());
    }

    public List<ServiceResponse> getFeaturedServices() {
        return serviceRepository.findByFeaturedIsTrue()
                .stream()
                .map(this::mapServiceToResponse)
                .collect(Collectors.toList());
    }

    public List<ServiceResponse> getServicesByCategory(String category) {
        return serviceRepository.findByCategory(category)
                .stream()
                .map(this::mapServiceToResponse)
                .collect(Collectors.toList());
    }

    public ServiceResponse getServiceById(String id) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + id));
        return mapServiceToResponse(service);
    }

    public ServiceResponse createService(ServiceRequest serviceRequest) {
        // Validate assigned staff IDs
        validateStaffIds(serviceRequest.getAssignedStaffIds());

        Service service = Service.builder()
                .name(serviceRequest.getName())
                .description(serviceRequest.getDescription())
                .category(serviceRequest.getCategory())
                .price(serviceRequest.getPrice())
                .duration(serviceRequest.getDuration())
                .imageUrls(serviceRequest.getImageUrls())
                .assignedStaffIds(serviceRequest.getAssignedStaffIds())
                .featured(serviceRequest.isFeatured())
                .active(serviceRequest.isActive())
                .availableMorning(serviceRequest.isAvailableMorning())
                .availableEvening(serviceRequest.isAvailableEvening())
                .createdAt(new Date())
                .updatedAt(new Date())
                .discountPercentage(serviceRequest.getDiscountPercentage())
                .discountStartDate(serviceRequest.getDiscountStartDate())
                .discountEndDate(serviceRequest.getDiscountEndDate())
                .build();

        Service savedService = serviceRepository.save(service);
        return mapServiceToResponse(savedService);
    }

    public ServiceResponse updateService(String id, ServiceRequest serviceRequest) {
        Service existingService = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + id));

        // Validate assigned staff IDs
        validateStaffIds(serviceRequest.getAssignedStaffIds());

        existingService.setName(serviceRequest.getName());
        existingService.setDescription(serviceRequest.getDescription());
        existingService.setCategory(serviceRequest.getCategory());
        existingService.setPrice(serviceRequest.getPrice());
        existingService.setDuration(serviceRequest.getDuration());
        existingService.setImageUrls(serviceRequest.getImageUrls());
        existingService.setAssignedStaffIds(serviceRequest.getAssignedStaffIds());
        existingService.setFeatured(serviceRequest.isFeatured());
        existingService.setActive(serviceRequest.isActive());
        existingService.setAvailableMorning(serviceRequest.isAvailableMorning());
        existingService.setAvailableEvening(serviceRequest.isAvailableEvening());
        existingService.setUpdatedAt(new Date());
        existingService.setDiscountPercentage(serviceRequest.getDiscountPercentage());
        existingService.setDiscountStartDate(serviceRequest.getDiscountStartDate());
        existingService.setDiscountEndDate(serviceRequest.getDiscountEndDate());

        Service updatedService = serviceRepository.save(existingService);
        return mapServiceToResponse(updatedService);
    }

    public void deleteService(String id) {
        if (!serviceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Service not found with id: " + id);
        }
        serviceRepository.deleteById(id);
    }

    // Helper method to validate staff IDs
    private void validateStaffIds(List<String> staffIds) {
        if (staffIds != null && !staffIds.isEmpty()) {
            for (String staffId : staffIds) {
                userRepository.findById(staffId)
                        .orElseThrow(() -> new ResourceNotFoundException("Staff not found with id: " + staffId));
            }
        }
    }

    // Helper method to map Service entity to ServiceResponse DTO
    private ServiceResponse mapServiceToResponse(Service service) {
        BigDecimal finalPrice = service.getPrice();

        // Calculate discount if applicable
        if (service.getDiscountPercentage() != null &&
                service.getDiscountStartDate() != null &&
                service.getDiscountEndDate() != null) {

            Date now = new Date();
            if (now.after(service.getDiscountStartDate()) && now.before(service.getDiscountEndDate())) {
                BigDecimal discountAmount = service.getPrice()
                        .multiply(service.getDiscountPercentage())
                        .divide(new BigDecimal("100"));
                finalPrice = service.getPrice().subtract(discountAmount);
            }
        }

        return ServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .category(service.getCategory())
                .price(service.getPrice())
                .finalPrice(finalPrice)
                .duration(service.getDuration())
                .imageUrls(service.getImageUrls())
                .assignedStaffIds(service.getAssignedStaffIds())
                .featured(service.isFeatured())
                .active(service.isActive())
                .availableMorning(service.isAvailableMorning())
                .availableEvening(service.isAvailableEvening())
                .createdAt(service.getCreatedAt())
                .updatedAt(service.getUpdatedAt())
                .discountPercentage(service.getDiscountPercentage())
                .discountStartDate(service.getDiscountStartDate())
                .discountEndDate(service.getDiscountEndDate())
                .build();
    }
}