package com.dis.workshopticketing.identityservice.service;

import com.dis.workshopticketing.identityservice.exception.DuplicateEmailException;
import com.dis.workshopticketing.identityservice.model.IdentityEmail;
import com.dis.workshopticketing.identityservice.model.IdentityEmailOwnerType;
import com.dis.workshopticketing.identityservice.repository.IdentityEmailRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailReservationServiceTest {

    @Mock
    private IdentityEmailRepository identityEmailRepository;

    @InjectMocks
    private EmailReservationService emailReservationService;

    @Test
    void normalizeTrimsAndConvertsEmailToLowercase() {
        String normalized = emailReservationService.normalize("  Ana@Example.COM  ");

        assertThat(normalized).isEqualTo("ana@example.com");
    }

    @Test
    void reserveSavesIdentityEmailWhenEmailIsAvailable() {
        when(identityEmailRepository.existsByEmail("ana@example.com")).thenReturn(false);

        emailReservationService.reserve("ana@example.com", IdentityEmailOwnerType.USER, 1L);

        ArgumentCaptor<IdentityEmail> captor = ArgumentCaptor.forClass(IdentityEmail.class);
        verify(identityEmailRepository).saveAndFlush(captor.capture());

        IdentityEmail savedEmail = captor.getValue();
        assertThat(savedEmail.getEmail()).isEqualTo("ana@example.com");
        assertThat(savedEmail.getOwnerType()).isEqualTo(IdentityEmailOwnerType.USER);
        assertThat(savedEmail.getOwnerId()).isEqualTo(1L);
    }

    @Test
    void reserveThrowsDuplicateEmailExceptionWhenEmailAlreadyExists() {
        when(identityEmailRepository.existsByEmail("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> emailReservationService.reserve("ana@example.com", IdentityEmailOwnerType.USER, 1L))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("ana@example.com");
    }

    @Test
    void reserveThrowsDuplicateEmailExceptionWhenDatabaseUniqueConstraintFails() {
        when(identityEmailRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(identityEmailRepository.saveAndFlush(any(IdentityEmail.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> emailReservationService.reserve("ana@example.com", IdentityEmailOwnerType.USER, 1L))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("ana@example.com");
    }
}
