package com.slimbahael.beauty_center.repository;

import com.slimbahael.beauty_center.model.Service;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends MongoRepository<Service, String> {

    List<Service> findByActiveIsTrue();

    List<Service> findByFeaturedIsTrue();

    List<Service> findByCategory(String category);

    List<Service> findByNameContainingIgnoreCase(String name);

    List<Service> findByAssignedStaffIdsContaining(String staffId);

    List<Service> findByAvailableMorningIsTrue();

    List<Service> findByAvailableEveningIsTrue();
}