package com.dis.workshopticketing.workshopservice.service;

import com.dis.workshopticketing.workshopservice.client.IdentityInstructorClient;
import com.dis.workshopticketing.workshopservice.dto.ExistenceResponse;
import com.dis.workshopticketing.workshopservice.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstructorValidationServiceTest {

    @Mock
    private IdentityInstructorClient identityInstructorClient;

    @InjectMocks
    private InstructorValidationService instructorValidationService;

    @Test
    void ensureInstructorExistsDoesNothingWhenInstructorExists() {
        when(identityInstructorClient.exists(1L)).thenReturn(new ExistenceResponse(1L, true));

        assertThatNoException().isThrownBy(() -> instructorValidationService.ensureInstructorExists(1L));
    }

    @Test
    void ensureInstructorExistsThrowsResourceNotFoundExceptionWhenInstructorDoesNotExist() {
        when(identityInstructorClient.exists(1L)).thenReturn(new ExistenceResponse(1L, false));

        assertThatThrownBy(() -> instructorValidationService.ensureInstructorExists(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Instructor not found: 1");
    }
}
