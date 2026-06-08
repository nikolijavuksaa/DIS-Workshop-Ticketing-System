package com.dis.workshopticketing.identityservice.service;

import com.dis.workshopticketing.identityservice.dto.CreateUserRequest;
import com.dis.workshopticketing.identityservice.dto.ExistenceResponse;
import com.dis.workshopticketing.identityservice.dto.UpdateUserRequest;
import com.dis.workshopticketing.identityservice.dto.UserResponse;
import com.dis.workshopticketing.identityservice.exception.ResourceNotFoundException;
import com.dis.workshopticketing.identityservice.model.IdentityEmailOwnerType;
import com.dis.workshopticketing.identityservice.model.User;
import com.dis.workshopticketing.identityservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailReservationService emailReservationService;

    @InjectMocks
    private UserService userService;

    @Test
    void createSavesActiveUserAndReservesEmail() {
        CreateUserRequest request = new CreateUserRequest("Ana", "Anic", "Ana@Example.COM", "060123456");
        when(emailReservationService.normalize("Ana@Example.COM")).thenReturn("ana@example.com");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        UserResponse response = userService.create(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User savedUser = captor.getValue();

        assertThat(savedUser.getFirstName()).isEqualTo("Ana");
        assertThat(savedUser.getLastName()).isEqualTo("Anic");
        assertThat(savedUser.getEmail()).isEqualTo("ana@example.com");
        assertThat(savedUser.getPhone()).isEqualTo("060123456");
        assertThat(savedUser.isActive()).isTrue();

        verify(emailReservationService).reserve("ana@example.com", IdentityEmailOwnerType.USER, 1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("ana@example.com");
        assertThat(response.active()).isTrue();
    }

    @Test
    void getAllReturnsActiveAndInactiveUsers() {
        User activeUser = user(1L, "Ana", "Anic", "ana@example.com", true);
        User inactiveUser = user(2L, "Marko", "Markovic", "marko@example.com", false);
        when(userRepository.findAll()).thenReturn(List.of(activeUser, inactiveUser));

        List<UserResponse> responses = userService.getAll();

        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(UserResponse::active)
                .containsExactly(true, false);
    }

    @Test
    void getReturnsActiveUser() {
        User user = user(1L, "Ana", "Anic", "ana@example.com", true);
        when(userRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.get(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.firstName()).isEqualTo("Ana");
        assertThat(response.active()).isTrue();
    }

    @Test
    void getThrowsResourceNotFoundExceptionWhenUserDoesNotExistOrIsInactive() {
        when(userRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.get(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found: 1");
    }

    @Test
    void updateChangesUserAndReservesEmailWhenEmailChanged() {
        User user = user(1L, "Ana", "Anic", "ana@example.com", true);
        UpdateUserRequest request = new UpdateUserRequest("Ana", "Updated", "ana2@example.com", "060999999");

        when(userRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(user));
        when(emailReservationService.normalize("ana2@example.com")).thenReturn("ana2@example.com");
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        UserResponse response = userService.update(1L, request);

        verify(emailReservationService).reserve("ana2@example.com", IdentityEmailOwnerType.USER, 1L);
        assertThat(user.getFirstName()).isEqualTo("Ana");
        assertThat(user.getLastName()).isEqualTo("Updated");
        assertThat(user.getEmail()).isEqualTo("ana2@example.com");
        assertThat(user.getPhone()).isEqualTo("060999999");
        assertThat(response.email()).isEqualTo("ana2@example.com");
    }

    @Test
    void updateDoesNotReserveEmailWhenEmailIsUnchanged() {
        User user = user(1L, "Ana", "Anic", "ana@example.com", true);
        UpdateUserRequest request = new UpdateUserRequest("Ana", "Updated", "ANA@example.com", "060999999");

        when(userRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(user));
        when(emailReservationService.normalize("ANA@example.com")).thenReturn("ana@example.com");
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        userService.update(1L, request);

        verify(emailReservationService, never()).reserve(any(), any(), any());
        assertThat(user.getLastName()).isEqualTo("Updated");
        assertThat(user.getEmail()).isEqualTo("ana@example.com");
    }

    @Test
    void deleteSetsActiveToFalse() {
        User user = user(1L, "Ana", "Anic", "ana@example.com", true);
        when(userRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(user));

        userService.delete(1L);

        assertThat(user.isActive()).isFalse();
    }

    @Test
    void existsReturnsRepositoryResult() {
        when(userRepository.existsByIdAndActiveTrue(1L)).thenReturn(true);

        ExistenceResponse response = userService.exists(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.exists()).isTrue();
    }

    private User user(Long id, String firstName, String lastName, String email, boolean active) {
        return User.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phone("060123456")
                .active(active)
                .build();
    }
}
