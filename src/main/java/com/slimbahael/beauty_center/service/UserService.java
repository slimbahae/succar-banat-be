package com.slimbahael.beauty_center.service;

import com.slimbahael.beauty_center.dto.CreateUserRequest;
import com.slimbahael.beauty_center.dto.UpdateUserRequest;
import com.slimbahael.beauty_center.dto.UserResponse;
import com.slimbahael.beauty_center.exception.ResourceAlreadyExistsException;
import com.slimbahael.beauty_center.exception.ResourceNotFoundException;
import com.slimbahael.beauty_center.model.User;
import com.slimbahael.beauty_center.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapUserToUserResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getUsersByRole(String role) {
        return userRepository.findByRole(role)
                .stream()
                .map(this::mapUserToUserResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapUserToUserResponse(user);
    }

    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email is already taken");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole())
                .profileImage(request.getProfileImage())
                .specialties(request.getSpecialties())
                .workDays(request.getWorkDays())
                .morningShift(request.getMorningShift())
                .eveningShift(request.getEveningShift())
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        return mapUserToUserResponse(savedUser);
    }

    public UserResponse updateUser(String id, UpdateUserRequest request) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Check if email is taken by another user
        if (!existingUser.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email is already taken");
        }

        existingUser.setFirstName(request.getFirstName());
        existingUser.setLastName(request.getLastName());
        existingUser.setEmail(request.getEmail());

        // Only update password if a new one is provided
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        existingUser.setPhoneNumber(request.getPhoneNumber());
        existingUser.setRole(request.getRole());
        existingUser.setProfileImage(request.getProfileImage());
        existingUser.setSpecialties(request.getSpecialties());
        existingUser.setWorkDays(request.getWorkDays());
        existingUser.setMorningShift(request.getMorningShift());
        existingUser.setEveningShift(request.getEveningShift());
        existingUser.setEnabled(request.isEnabled());

        User updatedUser = userRepository.save(existingUser);
        return mapUserToUserResponse(updatedUser);
    }

    public void deleteUser(String id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    // Helper method to map User entity to UserResponse DTO
    private UserResponse mapUserToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .profileImage(user.getProfileImage())
                .specialties(user.getSpecialties())
                .workDays(user.getWorkDays())
                .morningShift(user.getMorningShift())
                .eveningShift(user.getEveningShift())
                .enabled(user.isEnabled())
                .build();
    }
}
