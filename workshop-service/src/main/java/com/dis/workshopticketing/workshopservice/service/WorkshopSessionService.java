package com.dis.workshopticketing.workshopservice.service;

import com.dis.workshopticketing.workshopservice.dto.CreateWorkshopSessionRequest;
import com.dis.workshopticketing.workshopservice.dto.ExistenceResponse;
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

    public WorkshopSessionService(
            WorkshopSessionRepository workshopSessionRepository,
            WorkshopService workshopService
    ) {
        this.workshopSessionRepository = workshopSessionRepository;
        this.workshopService = workshopService;
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
                .active(true)
                .build();

        return WorkshopSessionResponse.from(workshopSessionRepository.saveAndFlush(session));
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

        return WorkshopSessionResponse.from(workshopSessionRepository.saveAndFlush(session));
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
