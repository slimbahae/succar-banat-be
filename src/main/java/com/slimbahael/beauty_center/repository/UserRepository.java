package com.slimbahael.beauty_center.repository;

import com.slimbahael.beauty_center.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(String role);

    // For finding staff by specialties (services they can perform)
    List<User> findByRoleAndSpecialtiesContaining(String role, String specialty);
}