package com.slimbahael.beauty_center.service;

import com.slimbahael.beauty_center.dto.ServiceAddonRequest;
import com.slimbahael.beauty_center.dto.ServiceAddonResponse;
import com.slimbahael.beauty_center.exception.ResourceNotFoundException;
import com.slimbahael.beauty_center.model.ServiceAddon;
import com.slimbahael.beauty_center.repository.ServiceAddonRepository;
import com.slimbahael.beauty_center.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceAddonService {

    private final ServiceAddonRepository serviceAddonRepository;
    private final ServiceRepository serviceRepository;

    public List<ServiceAddonResponse> getAllServiceAddons() {
        return serviceAddonRepository.findAll()
                .stream()
                .map(this::mapServiceAddonToResponse)
                .collect(Collectors.toList());
    }

    public List<ServiceAddonResponse> getActiveServiceAddons() {
        return serviceAddonRepository.findByActiveIsTrue()
                .stream()
                .map(this::mapServiceAddonToResponse)
                .collect(Collectors.toList());
    }

    public List<ServiceAddonResponse> getServiceAddonsByServiceId(String serviceId) {
        return serviceAddonRepository.findByCompatibleServiceIdsContaining(serviceId)
                .stream()
                .map(this::mapServiceAddonToResponse)
                .collect(Collectors.toList());
    }

    public ServiceAddonResponse getServiceAddonById(String id) {
        ServiceAddon serviceAddon = serviceAddonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service addon not found with id: " + id));
        return mapServiceAddonToResponse(serviceAddon);
    }

    public ServiceAddonResponse createServiceAddon(ServiceAddonRequest request) {
        // Validate compatible service IDs
        validateServiceIds(request.getCompatibleServiceIds());

        ServiceAddon serviceAddon = ServiceAddon.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .additionalDuration(request.getAdditionalDuration())
                .compatibleServiceIds(request.getCompatibleServiceIds())
                .active(request.isActive())
                .build();

        ServiceAddon savedServiceAddon = serviceAddonRepository.save(serviceAddon);
        return mapServiceAddonToResponse(savedServiceAddon);
    }

    public ServiceAddonResponse updateServiceAddon(String id, ServiceAddonRequest request) {
        ServiceAddon existingServiceAddon = serviceAddonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service addon not found with id: " + id));

        // Validate compatible service IDs
        validateServiceIds(request.getCompatibleServiceIds());

        existingServiceAddon.setName(request.getName());
        existingServiceAddon.setDescription(request.getDescription());
        existingServiceAddon.setPrice(request.getPrice());
        existingServiceAddon.setAdditionalDuration(request.getAdditionalDuration());
        existingServiceAddon.setCompatibleServiceIds(request.getCompatibleServiceIds());
        existingServiceAddon.setActive(request.isActive());

        ServiceAddon updatedServiceAddon = serviceAddonRepository.save(existingServiceAddon);
        return mapServiceAddonToResponse(updatedServiceAddon);
    }

    public void deleteServiceAddon(String id) {
        if (!serviceAddonRepository.existsById(id)) {
            throw new ResourceNotFoundException("Service addon not found with id: " + id);
        }
        serviceAddonRepository.deleteById(id);
    }

    // Helper method to validate service IDs
    private void validateServiceIds(List<String> serviceIds) {
        if (serviceIds != null && !serviceIds.isEmpty()) {
            for (String serviceId : serviceIds) {
                serviceRepository.findById(serviceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + serviceId));
            }
        }
    }

    // Helper method to map ServiceAddon entity to ServiceAddonResponse DTO
    private ServiceAddonResponse mapServiceAddonToResponse(ServiceAddon serviceAddon) {
        return ServiceAddonResponse.builder()
                .id(serviceAddon.getId())
                .name(serviceAddon.getName())
                .description(serviceAddon.getDescription())
                .price(serviceAddon.getPrice())
                .additionalDuration(serviceAddon.getAdditionalDuration())
                .compatibleServiceIds(serviceAddon.getCompatibleServiceIds())
                .active(serviceAddon.isActive())
                .build();
    }
}