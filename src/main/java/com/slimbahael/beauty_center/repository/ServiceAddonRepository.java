package com.slimbahael.beauty_center.repository;

import com.slimbahael.beauty_center.model.ServiceAddon;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceAddonRepository extends MongoRepository<ServiceAddon, String> {

    List<ServiceAddon> findByActiveIsTrue();

    List<ServiceAddon> findByCompatibleServiceIdsContaining(String serviceId);
}