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
import com.dis.workshopticketing.workshopservice.model.Workshop;
import com.dis.workshopticketing.workshopservice.model.WorkshopSession;
import com.dis.workshopticketing.workshopservice.repository.WorkshopSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkshopSessionService {

    private final WorkshopSessionRepository workshopSessionRepository;
    private final WorkshopService workshopService;
    private final ReservationInventoryClient reservationInventoryClient;

    public WorkshopSessionService(
            WorkshopSessionRepository workshopSessionRepository,
            WorkshopService workshopService,
            ReservationInventoryClient reservationInventoryClient
    ) {
        this.workshopSessionRepository = workshopSessionRepository;
        this.workshopService = workshopService;
        this.reservationInventoryClient = reservationInventoryClient;
    }

    @Transactional
    public WorkshopSessionResponse create(Long workshopId, CreateWorkshopSessionRequest request) {
        validateTimeRange(request.startsAt(), request.endsAt());
        Workshop workshop = workshopService.findActiveWorkshop(workshopId);

        WorkshopSession session = WorkshopSession.builder()
                .workshop(workshop)
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .location(request.location())
                .price(request.price())
                .capacity(request.capacity())
                .active(true)
                .build();

        WorkshopSession savedSession = workshopSessionRepository.saveAndFlush(session);
        reservationInventoryClient.createInventory(new CreateReservationInventoryRequest(
                savedSession.getId(),
                savedSession.getCapacity()
        ));

        return WorkshopSessionResponse.from(savedSession);
    }

    @Transactional(readOnly = true)
    public List<WorkshopSessionResponse> getAll() {
        return workshopSessionRepository.findAll()
                .stream()
                .map(WorkshopSessionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WorkshopSessionResponse> getAllByWorkshop(Long workshopId) {
        workshopService.findActiveWorkshop(workshopId);

        return workshopSessionRepository.findAllByWorkshopId(workshopId)
                .stream()
                .map(WorkshopSessionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkshopSessionResponse get(Long id) {
        return WorkshopSessionResponse.from(findActiveSession(id));
    }

    @Transactional
    public WorkshopSessionResponse update(Long id, UpdateWorkshopSessionRequest request) {
        validateTimeRange(request.startsAt(), request.endsAt());
        WorkshopSession session = findActiveSession(id);

        session.setStartsAt(request.startsAt());
        session.setEndsAt(request.endsAt());
        session.setLocation(request.location());
        session.setPrice(request.price());
        session.setCapacity(request.capacity());

        WorkshopSession savedSession = workshopSessionRepository.saveAndFlush(session);
        reservationInventoryClient.updateCapacity(
                savedSession.getId(),
                new UpdateReservationInventoryCapacityRequest(savedSession.getCapacity())
        );

        return WorkshopSessionResponse.from(savedSession);
    }

    @Transactional
    public void delete(Long id) {
        WorkshopSession session = findActiveSession(id);
        session.setActive(false);
    }

    @Transactional(readOnly = true)
    public ExistenceResponse exists(Long id) {
        return new ExistenceResponse(id, workshopSessionRepository.existsByIdAndActiveTrueAndWorkshopActiveTrue(id));
    }

    private WorkshopSession findActiveSession(Long id) {
        return workshopSessionRepository.findByIdAndActiveTrueAndWorkshopActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop session", id));
    }

    private void validateTimeRange(java.time.LocalDateTime startsAt, java.time.LocalDateTime endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw new BadRequestException("endsAt must be after startsAt");
        }
    }
}
