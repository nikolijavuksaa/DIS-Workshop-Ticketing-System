package com.dis.workshopticketing.identityservice.service;

import com.dis.workshopticketing.identityservice.exception.DuplicateEmailException;
import com.dis.workshopticketing.identityservice.model.IdentityEmail;
import com.dis.workshopticketing.identityservice.model.IdentityEmailOwnerType;
import com.dis.workshopticketing.identityservice.repository.IdentityEmailRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class EmailReservationService {

    private final IdentityEmailRepository identityEmailRepository;

    public EmailReservationService(IdentityEmailRepository identityEmailRepository) {
        this.identityEmailRepository = identityEmailRepository;
    }

    public String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public void reserve(String email, IdentityEmailOwnerType ownerType, Long ownerId) {
        if (identityEmailRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        try {
            identityEmailRepository.saveAndFlush(IdentityEmail.builder()
                    .email(email)
                    .ownerType(ownerType)
                    .ownerId(ownerId)
                    .build());
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException(email);
        }
    }
}
