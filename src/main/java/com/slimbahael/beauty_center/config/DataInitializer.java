package com.slimbahael.beauty_center.config;

import com.slimbahael.beauty_center.model.*;
import com.slimbahael.beauty_center.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
@RequiredArgsConstructor
@Profile("dev") // Only run in development mode
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceAddonRepository serviceAddonRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Only initialize if collections are empty
        if (userRepository.count() == 0) {
            createUsers();
        }

        if (productRepository.count() == 0) {
            createProducts();
        }

        if (serviceRepository.count() == 0) {
            createServices();
        }

        if (serviceAddonRepository.count() == 0) {
            createServiceAddons();
        }
    }

    private void createUsers() {
        // Create Admin user
        User admin = User.builder()
                .firstName("Admin")
                .lastName("User")
                .email("admin@beautycenter.com")
                .password(passwordEncoder.encode("admin123"))
                .phoneNumber("1234567890")
                .role("ADMIN")
                .enabled(true)
                .build();

        // Create Staff members
        User staff1 = User.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@beautycenter.com")
                .password(passwordEncoder.encode("staff123"))
                .phoneNumber("2345678901")
                .role("STAFF")
                .enabled(true)
                .specialties(Arrays.asList("haircut", "coloring", "styling"))
                .workDays(Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"))
                .morningShift("YES")
                .eveningShift("NO")
                .build();

        User staff2 = User.builder()
                .firstName("John")
                .lastName("Davis")
                .email("john.davis@beautycenter.com")
                .password(passwordEncoder.encode("staff123"))
                .phoneNumber("3456789012")
                .role("STAFF")
                .enabled(true)
                .specialties(Arrays.asList("facial", "massage", "waxing"))
                .workDays(Arrays.asList("MONDAY", "WEDNESDAY", "FRIDAY", "SATURDAY", "SUNDAY"))
                .morningShift("NO")
                .eveningShift("YES")
                .build();

        // Create Customer
        User customer = User.builder()
                .firstName("Alice")
                .lastName("Johnson")
                .email("alice@example.com")
                .password(passwordEncoder.encode("customer123"))
                .phoneNumber("4567890123")
                .role("CUSTOMER")
                .enabled(true)
                .build();

        userRepository.saveAll(Arrays.asList(admin, staff1, staff2, customer));
    }

    private void createProducts() {
        // Create sample products
        List<Product> products = new ArrayList<>();

        Product product1 = Product.builder()
                .name("Premium Shampoo")
                .description("Luxurious shampoo for all hair types")
                .category("Hair Care")
                .price(new BigDecimal("24.99"))
                .stockQuantity(50)
                .imageUrls(Collections.singletonList("https://via.placeholder.com/300"))
                .tags(Arrays.asList("shampoo", "hair care", "premium"))
                .brand("LuxHair")
                .sku("SH-PREMIUM-001")
                .featured(true)
                .active(true)
                .createdAt(new Date())
                .updatedAt(new Date())
                .specifications(Arrays.asList(
                        Product.ProductSpecification.builder().name("Volume").value("300ml").build(),
                        Product.ProductSpecification.builder().name("Hair Type").value("All Types").build()
                ))
                .build();

        Product product2 = Product.builder()
                .name("Argan Oil Hair Mask")
                .description("Deep conditioning hair mask with argan oil")
                .category("Hair Care")
                .price(new BigDecimal("32.50"))
                .stockQuantity(30)
                .imageUrls(Collections.singletonList("https://via.placeholder.com/300"))
                .tags(Arrays.asList("hair mask", "conditioning", "argan oil"))
                .brand("NaturOil")
                .sku("HM-ARGAN-002")
                .featured(false)
                .active(true)
                .createdAt(new Date())
                .updatedAt(new Date())
                .specifications(Arrays.asList(
                        Product.ProductSpecification.builder().name("Volume").value("250ml").build(),
                        Product.ProductSpecification.builder().name("Hair Type").value("Dry, Damaged").build()
                ))
                .build();

        Product product3 = Product.builder()
                .name("Anti-Aging Face Cream")
                .description("Premium face cream with anti-aging properties")
                .category("Skin Care")
                .price(new BigDecimal("59.99"))
                .stockQuantity(20)
                .imageUrls(Collections.singletonList("https://via.placeholder.com/300"))
                .tags(Arrays.asList("face cream", "anti-aging", "moisturizer"))
                .brand("DermaPro")
                .sku("SC-ANTIAGE-003")
                .featured(true)
                .active(true)
                .createdAt(new Date())
                .updatedAt(new Date())
                .specifications(Arrays.asList(
                        Product.ProductSpecification.builder().name("Volume").value("50ml").build(),
                        Product.ProductSpecification.builder().name("Skin Type").value("All Types").build(),
                        Product.ProductSpecification.builder().name("Benefits").value("Anti-aging, Hydrating").build()
                ))
                .build();

        products.add(product1);
        products.add(product2);
        products.add(product3);

        productRepository.saveAll(products);
    }

    private void createServices() {
        // Create sample services
        List<Service> services = new ArrayList<>();

        // Get staff IDs
        List<User> staffMembers = userRepository.findByRole("STAFF");
        List<String> staffIds = new ArrayList<>();
        staffMembers.forEach(staff -> staffIds.add(staff.getId()));

        Service service1 = Service.builder()
                .name("Haircut & Styling")
                .description("Professional haircut and styling service")
                .category("Hair Services")
                .price(new BigDecimal("50.00"))
                .duration(60) // 60 minutes
                .imageUrls(Collections.singletonList("https://via.placeholder.com/300"))
                .assignedStaffIds(Collections.singletonList(staffIds.get(0))) // Jane Smith
                .featured(true)
                .active(true)
                .availableMorning(true)
                .availableEvening(true)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        Service service2 = Service.builder()
                .name("Hair Coloring")
                .description("Professional hair coloring service")
                .category("Hair Services")
                .price(new BigDecimal("85.00"))
                .duration(120) // 120 minutes
                .imageUrls(Collections.singletonList("https://via.placeholder.com/300"))
                .assignedStaffIds(Collections.singletonList(staffIds.get(0))) // Jane Smith
                .featured(false)
                .active(true)
                .availableMorning(true)
                .availableEvening(false)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        Service service3 = Service.builder()
                .name("Facial Treatment")
                .description("Rejuvenating facial treatment for all skin types")
                .category("Skin Care")
                .price(new BigDecimal("70.00"))
                .duration(60) // 60 minutes
                .imageUrls(Collections.singletonList("https://via.placeholder.com/300"))
                .assignedStaffIds(Collections.singletonList(staffIds.get(1))) // John Davis
                .featured(true)
                .active(true)
                .availableMorning(false)
                .availableEvening(true)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        Service service4 = Service.builder()
                .name("Full Body Massage")
                .description("Relaxing full body massage")
                .category("Massage")
                .price(new BigDecimal("90.00"))
                .duration(90) // 90 minutes
                .imageUrls(Collections.singletonList("https://via.placeholder.com/300"))
                .assignedStaffIds(Collections.singletonList(staffIds.get(1))) // John Davis
                .featured(false)
                .active(true)
                .availableMorning(false)
                .availableEvening(true)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        services.add(service1);
        services.add(service2);
        services.add(service3);
        services.add(service4);

        serviceRepository.saveAll(services);
    }

    private void createServiceAddons() {
        // Create sample service add-ons
        List<ServiceAddon> addons = new ArrayList<>();

        // Get service IDs
        List<Service> allServices = serviceRepository.findAll();

        List<String> hairServiceIds = new ArrayList<>();
        List<String> skinServiceIds = new ArrayList<>();
        List<String> massageServiceIds = new ArrayList<>();

        for (Service service : allServices) {
            if (service.getCategory().equals("Hair Services")) {
                hairServiceIds.add(service.getId());
            } else if (service.getCategory().equals("Skin Care")) {
                skinServiceIds.add(service.getId());
            } else if (service.getCategory().equals("Massage")) {
                massageServiceIds.add(service.getId());
            }
        }

        ServiceAddon addon1 = ServiceAddon.builder()
                .name("Deep Conditioning Treatment")
                .description("Hydrating treatment for dry or damaged hair")
                .price(new BigDecimal("20.00"))
                .additionalDuration(15) // 15 minutes
                .compatibleServiceIds(hairServiceIds)
                .active(true)
                .build();

        ServiceAddon addon2 = ServiceAddon.builder()
                .name("Hair Protein Treatment")
                .description("Strengthening protein treatment for weak hair")
                .price(new BigDecimal("25.00"))
                .additionalDuration(20) // 20 minutes
                .compatibleServiceIds(hairServiceIds)
                .active(true)
                .build();

        ServiceAddon addon3 = ServiceAddon.builder()
                .name("Anti-Aging Mask")
                .description("Premium anti-aging mask for facial treatments")
                .price(new BigDecimal("30.00"))
                .additionalDuration(15) // 15 minutes
                .compatibleServiceIds(skinServiceIds)
                .active(true)
                .build();

        ServiceAddon addon4 = ServiceAddon.builder()
                .name("Hot Stone Add-On")
                .description("Hot stone therapy to enhance your massage")
                .price(new BigDecimal("35.00"))
                .additionalDuration(15) // 15 minutes
                .compatibleServiceIds(massageServiceIds)
                .active(true)
                .build();

        addons.add(addon1);
        addons.add(addon2);
        addons.add(addon3);
        addons.add(addon4);

        serviceAddonRepository.saveAll(addons);
    }
}