package com.dis.workshopticketing.identityservice.security;

import com.dis.workshopticketing.identityservice.model.AuthProvider;
import com.dis.workshopticketing.identityservice.model.IdentityEmailOwnerType;
import com.dis.workshopticketing.identityservice.model.User;
import com.dis.workshopticketing.identityservice.model.UserRole;
import com.dis.workshopticketing.identityservice.repository.UserRepository;
import com.dis.workshopticketing.identityservice.service.EmailReservationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuth2IdentityService {

    private final UserRepository userRepository;
    private final EmailReservationService emailReservationService;

    public OAuth2IdentityService(UserRepository userRepository, EmailReservationService emailReservationService) {
        this.userRepository = userRepository;
        this.emailReservationService = emailReservationService;
    }

    @Transactional
    public User upsertOAuth2User(
            String registrationId,
            String providerId,
            String email,
            String firstName,
            String lastName
    ) {
        String normalizedEmail = emailReservationService.normalize(email);
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        return userRepository.findByEmailAndActiveTrue(normalizedEmail)
                .map(user -> updateExistingUser(user, provider, providerId, firstName, lastName))
                .orElseGet(() -> createUser(provider, providerId, normalizedEmail, firstName, lastName));
    }

    private User updateExistingUser(
            User user,
            AuthProvider provider,
            String providerId,
            String firstName,
            String lastName
    ) {
        user.setAuthProvider(provider);
        user.setProviderId(providerId);
        if (user.getFirstName() == null || user.getFirstName().isBlank()) {
            user.setFirstName(firstName);
        }
        if (user.getLastName() == null || user.getLastName().isBlank()) {
            user.setLastName(lastName);
        }
        return userRepository.saveAndFlush(user);
    }

    private User createUser(
            AuthProvider provider,
            String providerId,
            String email,
            String firstName,
            String lastName
    ) {
        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .role(UserRole.USER)
                .authProvider(provider)
                .providerId(providerId)
                .active(true)
                .build();

        User savedUser = userRepository.saveAndFlush(user);
        emailReservationService.reserve(email, IdentityEmailOwnerType.USER, savedUser.getId());
        return savedUser;
    }
}
