package com.dis.workshopticketing.identityservice.service;

import com.dis.workshopticketing.identityservice.dto.CreateUserRequest;
import com.dis.workshopticketing.identityservice.dto.ExistenceResponse;
import com.dis.workshopticketing.identityservice.dto.UpdateUserRequest;
import com.dis.workshopticketing.identityservice.dto.UserResponse;
import com.dis.workshopticketing.identityservice.exception.ResourceNotFoundException;
import com.dis.workshopticketing.identityservice.model.AuthProvider;
import com.dis.workshopticketing.identityservice.model.IdentityEmailOwnerType;
import com.dis.workshopticketing.identityservice.model.User;
import com.dis.workshopticketing.identityservice.model.UserRole;
import com.dis.workshopticketing.identityservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final EmailReservationService emailReservationService;

    public UserService(UserRepository userRepository, EmailReservationService emailReservationService) {
        this.userRepository = userRepository;
        this.emailReservationService = emailReservationService;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = emailReservationService.normalize(request.email());
        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(email)
                .phone(request.phone())
                .role(UserRole.USER)
                .authProvider(AuthProvider.LOCAL)
                .active(true)
                .build();
        User savedUser = userRepository.saveAndFlush(user);

        emailReservationService.reserve(email, IdentityEmailOwnerType.USER, savedUser.getId());

        return UserResponse.from(savedUser);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return UserResponse.from(findActiveUser(id));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = findActiveUser(id);
        String email = emailReservationService.normalize(request.email());

        if (!email.equals(user.getEmail())) {
            emailReservationService.reserve(email, IdentityEmailOwnerType.USER, user.getId());
            user.setEmail(email);
        }

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());

        return UserResponse.from(userRepository.saveAndFlush(user));
    }

    @Transactional
    public void delete(Long id) {
        User user = findActiveUser(id);
        user.setActive(false);
    }

    @Transactional(readOnly = true)
    public ExistenceResponse exists(Long id) {
        return new ExistenceResponse(id, userRepository.existsByIdAndActiveTrue(id));
    }

    private User findActiveUser(Long id) {
        return userRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
