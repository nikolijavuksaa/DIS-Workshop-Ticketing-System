package com.dis.workshopticketing.workshopservice.service;

import com.dis.workshopticketing.workshopservice.client.ReservationInventoryClient;
import com.dis.workshopticketing.workshopservice.dto.CreateReservationInventoryRequest;
import com.dis.workshopticketing.workshopservice.dto.CreateWorkshopSessionRequest;
import com.dis.workshopticketing.workshopservice.dto.ExistenceResponse;
import com.dis.workshopticketing.workshopservice.dto.UpdateReservationInventoryCapacityRequest;
import com.dis.workshopticketing.workshopservice.dto.UpdateWorkshopSessionRequest;
import com.dis.workshopticketing.workshopservice.dto.WorkshopSessionResponse;
import com.dis.workshopticketing.workshopservice.exception.BadRequestException;
import com.dis.workshopticketing.workshopservice.exception.ResourceNotFoundException;
import com.dis.workshopticketing.workshopservice.model.Category;
import com.dis.workshopticketing.workshopservice.model.Workshop;
import com.dis.workshopticketing.workshopservice.model.WorkshopSession;
import com.dis.workshopticketing.workshopservice.repository.WorkshopSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkshopSessionServiceTest {

    @Mock
    private WorkshopSessionRepository workshopSessionRepository;

    @Mock
    private WorkshopService workshopService;

    @Mock
    private ReservationInventoryClient reservationInventoryClient;

    @InjectMocks
    private WorkshopSessionService workshopSessionService;

    @Test
    void createSavesActiveSessionForActiveWorkshop() {
        Workshop workshop = workshop(1L, true);
        CreateWorkshopSessionRequest request = new CreateWorkshopSessionRequest(
                startsAt(),
                endsAt(),
                "Studio 12, Belgrade",
                new BigDecimal("3500.00"),
                20
        );

        when(workshopService.findActiveWorkshop(1L)).thenReturn(workshop);
        when(workshopSessionRepository.saveAndFlush(any(WorkshopSession.class))).thenAnswer(invocation -> {
            WorkshopSession session = invocation.getArgument(0);
            session.setId(1L);
            return session;
        });

        WorkshopSessionResponse response = workshopSessionService.create(1L, request);

        verify(workshopService).findActiveWorkshop(1L);

        ArgumentCaptor<WorkshopSession> captor = ArgumentCaptor.forClass(WorkshopSession.class);
        verify(workshopSessionRepository).saveAndFlush(captor.capture());
        WorkshopSession savedSession = captor.getValue();

        assertThat(savedSession.getWorkshop()).isEqualTo(workshop);
        assertThat(savedSession.getStartsAt()).isEqualTo(startsAt());
        assertThat(savedSession.getEndsAt()).isEqualTo(endsAt());
        assertThat(savedSession.getLocation()).isEqualTo("Studio 12, Belgrade");
        assertThat(savedSession.getPrice()).isEqualByComparingTo("3500.00");
        assertThat(savedSession.getCapacity()).isEqualTo(20);
        assertThat(savedSession.isActive()).isTrue();

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.workshopId()).isEqualTo(1L);
        verify(reservationInventoryClient).createInventory(new CreateReservationInventoryRequest(1L, 20));
    }

    @Test
    void createThrowsBadRequestExceptionWhenEndsAtIsNotAfterStartsAt() {
        CreateWorkshopSessionRequest request = new CreateWorkshopSessionRequest(
                startsAt(),
                startsAt(),
                "Studio 12, Belgrade",
                new BigDecimal("3500.00"),
                20
        );

        assertThatThrownBy(() -> workshopSessionService.create(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("endsAt must be after startsAt");
    }

    @Test
    void getAllReturnsActiveAndInactiveSessions() {
        Workshop workshop = workshop(1L, true);
        WorkshopSession activeSession = session(1L, workshop, true);
        WorkshopSession inactiveSession = session(2L, workshop, false);
        when(workshopSessionRepository.findAll()).thenReturn(List.of(activeSession, inactiveSession));

        List<WorkshopSessionResponse> responses = workshopSessionService.getAll();

        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(WorkshopSessionResponse::active)
                .containsExactly(true, false);
    }

    @Test
    void getAllByWorkshopChecksWorkshopAndReturnsAllWorkshopSessions() {
        Workshop workshop = workshop(1L, true);
        WorkshopSession firstSession = session(1L, workshop, true);
        WorkshopSession secondSession = session(2L, workshop, false);

        when(workshopService.findActiveWorkshop(1L)).thenReturn(workshop);
        when(workshopSessionRepository.findAllByWorkshopId(1L)).thenReturn(List.of(firstSession, secondSession));

        List<WorkshopSessionResponse> responses = workshopSessionService.getAllByWorkshop(1L);

        verify(workshopService).findActiveWorkshop(1L);
        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(WorkshopSessionResponse::active)
                .containsExactly(true, false);
    }

    @Test
    void getReturnsSessionOnlyWhenSessionAndParentWorkshopAreActive() {
        Workshop workshop = workshop(1L, true);
        WorkshopSession session = session(1L, workshop, true);
        when(workshopSessionRepository.findByIdAndActiveTrueAndWorkshopActiveTrue(1L))
                .thenReturn(Optional.of(session));

        WorkshopSessionResponse response = workshopSessionService.get(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.workshopId()).isEqualTo(1L);
        assertThat(response.active()).isTrue();
    }

    @Test
    void getThrowsResourceNotFoundExceptionWhenSessionDoesNotExistOrParentWorkshopIsInactive() {
        when(workshopSessionRepository.findByIdAndActiveTrueAndWorkshopActiveTrue(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> workshopSessionService.get(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Workshop session not found: 1");
    }

    @Test
    void updateChangesTimeLocationAndPriceWhenTimeRangeIsValid() {
        Workshop workshop = workshop(1L, true);
        WorkshopSession session = session(1L, workshop, true);
        UpdateWorkshopSessionRequest request = new UpdateWorkshopSessionRequest(
                startsAt().plusDays(1),
                endsAt().plusDays(1),
                "Studio 15, Belgrade",
                new BigDecimal("4200.00"),
                25
        );

        when(workshopSessionRepository.findByIdAndActiveTrueAndWorkshopActiveTrue(1L))
                .thenReturn(Optional.of(session));
        when(workshopSessionRepository.saveAndFlush(session)).thenReturn(session);

        WorkshopSessionResponse response = workshopSessionService.update(1L, request);

        assertThat(session.getStartsAt()).isEqualTo(startsAt().plusDays(1));
        assertThat(session.getEndsAt()).isEqualTo(endsAt().plusDays(1));
        assertThat(session.getLocation()).isEqualTo("Studio 15, Belgrade");
        assertThat(session.getPrice()).isEqualByComparingTo("4200.00");
        assertThat(session.getCapacity()).isEqualTo(25);
        assertThat(response.location()).isEqualTo("Studio 15, Belgrade");
        verify(reservationInventoryClient).updateCapacity(1L, new UpdateReservationInventoryCapacityRequest(25));
    }

    @Test
    void updateThrowsBadRequestExceptionWhenTimeRangeIsInvalid() {
        UpdateWorkshopSessionRequest request = new UpdateWorkshopSessionRequest(
                endsAt(),
                startsAt(),
                "Studio 15, Belgrade",
                new BigDecimal("4200.00"),
                25
        );

        assertThatThrownBy(() -> workshopSessionService.update(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("endsAt must be after startsAt");
    }

    @Test
    void deleteSetsActiveToFalse() {
        Workshop workshop = workshop(1L, true);
        WorkshopSession session = session(1L, workshop, true);
        when(workshopSessionRepository.findByIdAndActiveTrueAndWorkshopActiveTrue(1L))
                .thenReturn(Optional.of(session));

        workshopSessionService.delete(1L);

        assertThat(session.isActive()).isFalse();
    }

    @Test
    void existsReturnsRepositoryResultThatChecksSessionAndParentWorkshopActive() {
        when(workshopSessionRepository.existsByIdAndActiveTrueAndWorkshopActiveTrue(1L)).thenReturn(true);

        ExistenceResponse response = workshopSessionService.exists(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.exists()).isTrue();
    }

    private WorkshopSession session(Long id, Workshop workshop, boolean active) {
        return WorkshopSession.builder()
                .id(id)
                .workshop(workshop)
                .startsAt(startsAt())
                .endsAt(endsAt())
                .location("Studio 12, Belgrade")
                .price(new BigDecimal("3500.00"))
                .capacity(20)
                .active(active)
                .build();
    }

    private Workshop workshop(Long id, boolean active) {
        Category category = Category.builder()
                .id(1L)
                .name("Painting")
                .description("Painting workshops")
                .active(true)
                .build();

        return Workshop.builder()
                .id(id)
                .title("Beginner Watercolor")
                .description("Intro workshop")
                .instructorId(10L)
                .category(category)
                .active(active)
                .build();
    }

    private LocalDateTime startsAt() {
        return LocalDateTime.of(2026, 6, 10, 18, 0);
    }

    private LocalDateTime endsAt() {
        return LocalDateTime.of(2026, 6, 10, 20, 0);
    }
}
