package com.dis.workshopticketing.identityservice.service;

import com.dis.workshopticketing.identityservice.dto.CreateInstructorRequest;
import com.dis.workshopticketing.identityservice.dto.ExistenceResponse;
import com.dis.workshopticketing.identityservice.dto.InstructorResponse;
import com.dis.workshopticketing.identityservice.dto.UpdateInstructorRequest;
import com.dis.workshopticketing.identityservice.exception.ResourceNotFoundException;
import com.dis.workshopticketing.identityservice.model.IdentityEmailOwnerType;
import com.dis.workshopticketing.identityservice.model.Instructor;
import com.dis.workshopticketing.identityservice.repository.InstructorRepository;
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
class InstructorServiceTest {

    @Mock
    private InstructorRepository instructorRepository;

    @Mock
    private EmailReservationService emailReservationService;

    @InjectMocks
    private InstructorService instructorService;

    @Test
    void createSavesActiveInstructorAndReservesEmail() {
        CreateInstructorRequest request = new CreateInstructorRequest(
                "Mila",
                "Milic",
                "Mila@Example.COM",
                "Visual artist",
                "Watercolor"
        );
        when(emailReservationService.normalize("Mila@Example.COM")).thenReturn("mila@example.com");
        when(instructorRepository.saveAndFlush(any(Instructor.class))).thenAnswer(invocation -> {
            Instructor instructor = invocation.getArgument(0);
            instructor.setId(1L);
            return instructor;
        });

        InstructorResponse response = instructorService.create(request);

        ArgumentCaptor<Instructor> captor = ArgumentCaptor.forClass(Instructor.class);
        verify(instructorRepository).saveAndFlush(captor.capture());
        Instructor savedInstructor = captor.getValue();

        assertThat(savedInstructor.getFirstName()).isEqualTo("Mila");
        assertThat(savedInstructor.getLastName()).isEqualTo("Milic");
        assertThat(savedInstructor.getEmail()).isEqualTo("mila@example.com");
        assertThat(savedInstructor.getBio()).isEqualTo("Visual artist");
        assertThat(savedInstructor.getExpertise()).isEqualTo("Watercolor");
        assertThat(savedInstructor.isActive()).isTrue();

        verify(emailReservationService).reserve("mila@example.com", IdentityEmailOwnerType.INSTRUCTOR, 1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("mila@example.com");
        assertThat(response.active()).isTrue();
    }

    @Test
    void getAllReturnsActiveAndInactiveInstructors() {
        Instructor activeInstructor = instructor(1L, "Mila", "Milic", "mila@example.com", true);
        Instructor inactiveInstructor = instructor(2L, "Petar", "Petrovic", "petar@example.com", false);
        when(instructorRepository.findAll()).thenReturn(List.of(activeInstructor, inactiveInstructor));

        List<InstructorResponse> responses = instructorService.getAll();

        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(InstructorResponse::active)
                .containsExactly(true, false);
    }

    @Test
    void getReturnsActiveInstructor() {
        Instructor instructor = instructor(1L, "Mila", "Milic", "mila@example.com", true);
        when(instructorRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(instructor));

        InstructorResponse response = instructorService.get(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.firstName()).isEqualTo("Mila");
        assertThat(response.active()).isTrue();
    }

    @Test
    void getThrowsResourceNotFoundExceptionWhenInstructorDoesNotExistOrIsInactive() {
        when(instructorRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> instructorService.get(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Instructor not found: 1");
    }

    @Test
    void updateChangesInstructorAndReservesEmailWhenEmailChanged() {
        Instructor instructor = instructor(1L, "Mila", "Milic", "mila@example.com", true);
        UpdateInstructorRequest request = new UpdateInstructorRequest(
                "Mila",
                "Updated",
                "mila2@example.com",
                "Updated bio",
                "Ceramics"
        );

        when(instructorRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(instructor));
        when(emailReservationService.normalize("mila2@example.com")).thenReturn("mila2@example.com");
        when(instructorRepository.saveAndFlush(instructor)).thenReturn(instructor);

        InstructorResponse response = instructorService.update(1L, request);

        verify(emailReservationService).reserve("mila2@example.com", IdentityEmailOwnerType.INSTRUCTOR, 1L);
        assertThat(instructor.getFirstName()).isEqualTo("Mila");
        assertThat(instructor.getLastName()).isEqualTo("Updated");
        assertThat(instructor.getEmail()).isEqualTo("mila2@example.com");
        assertThat(instructor.getBio()).isEqualTo("Updated bio");
        assertThat(instructor.getExpertise()).isEqualTo("Ceramics");
        assertThat(response.email()).isEqualTo("mila2@example.com");
    }

    @Test
    void updateDoesNotReserveEmailWhenEmailIsUnchanged() {
        Instructor instructor = instructor(1L, "Mila", "Milic", "mila@example.com", true);
        UpdateInstructorRequest request = new UpdateInstructorRequest(
                "Mila",
                "Updated",
                "MILA@example.com",
                "Updated bio",
                "Ceramics"
        );

        when(instructorRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(instructor));
        when(emailReservationService.normalize("MILA@example.com")).thenReturn("mila@example.com");
        when(instructorRepository.saveAndFlush(instructor)).thenReturn(instructor);

        instructorService.update(1L, request);

        verify(emailReservationService, never()).reserve(any(), any(), any());
        assertThat(instructor.getLastName()).isEqualTo("Updated");
        assertThat(instructor.getEmail()).isEqualTo("mila@example.com");
        assertThat(instructor.getBio()).isEqualTo("Updated bio");
        assertThat(instructor.getExpertise()).isEqualTo("Ceramics");
    }

    @Test
    void deleteSetsActiveToFalse() {
        Instructor instructor = instructor(1L, "Mila", "Milic", "mila@example.com", true);
        when(instructorRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(instructor));

        instructorService.delete(1L);

        assertThat(instructor.isActive()).isFalse();
    }

    @Test
    void existsReturnsRepositoryResult() {
        when(instructorRepository.existsByIdAndActiveTrue(1L)).thenReturn(true);

        ExistenceResponse response = instructorService.exists(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.exists()).isTrue();
    }

    private Instructor instructor(Long id, String firstName, String lastName, String email, boolean active) {
        return Instructor.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .bio("Visual artist")
                .expertise("Watercolor")
                .active(active)
                .build();
    }
}
