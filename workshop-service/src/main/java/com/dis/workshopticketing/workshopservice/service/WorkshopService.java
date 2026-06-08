package com.dis.workshopticketing.workshopservice.service;

import com.dis.workshopticketing.workshopservice.dto.CreateWorkshopRequest;
import com.dis.workshopticketing.workshopservice.dto.UpdateWorkshopRequest;
import com.dis.workshopticketing.workshopservice.dto.WorkshopResponse;
import com.dis.workshopticketing.workshopservice.exception.ResourceNotFoundException;
import com.dis.workshopticketing.workshopservice.model.Category;
import com.dis.workshopticketing.workshopservice.model.Workshop;
import com.dis.workshopticketing.workshopservice.repository.WorkshopRepository;
import com.dis.workshopticketing.workshopservice.repository.WorkshopSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkshopService {

    private final WorkshopRepository workshopRepository;
    private final WorkshopSessionRepository workshopSessionRepository;
    private final CategoryService categoryService;
    private final InstructorValidationService instructorValidationService;

    public WorkshopService(
            WorkshopRepository workshopRepository,
            WorkshopSessionRepository workshopSessionRepository,
            CategoryService categoryService,
            InstructorValidationService instructorValidationService
    ) {
        this.workshopRepository = workshopRepository;
        this.workshopSessionRepository = workshopSessionRepository;
        this.categoryService = categoryService;
        this.instructorValidationService = instructorValidationService;
    }

    @Transactional
    public WorkshopResponse create(CreateWorkshopRequest request) {
        instructorValidationService.ensureInstructorExists(request.instructorId());
        Category category = categoryService.findActiveCategory(request.categoryId());

        Workshop workshop = Workshop.builder()
                .title(request.title())
                .description(request.description())
                .instructorId(request.instructorId())
                .category(category)
                .active(true)
                .build();

        return WorkshopResponse.from(workshopRepository.saveAndFlush(workshop));
    }

    @Transactional(readOnly = true)
    public List<WorkshopResponse> getAll() {
        return workshopRepository.findAll()
                .stream()
                .map(WorkshopResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkshopResponse get(Long id) {
        return WorkshopResponse.from(findActiveWorkshop(id));
    }

    @Transactional
    public WorkshopResponse update(Long id, UpdateWorkshopRequest request) {
        Workshop workshop = findActiveWorkshop(id);
        instructorValidationService.ensureInstructorExists(request.instructorId());
        Category category = categoryService.findActiveCategory(request.categoryId());

        workshop.setTitle(request.title());
        workshop.setDescription(request.description());
        workshop.setInstructorId(request.instructorId());
        workshop.setCategory(category);

        return WorkshopResponse.from(workshopRepository.saveAndFlush(workshop));
    }

    @Transactional
    public void delete(Long id) {
        Workshop workshop = findActiveWorkshop(id);
        workshop.setActive(false);
        workshopSessionRepository.findAllByWorkshopIdAndActiveTrue(id)
                .forEach(session -> session.setActive(false));
    }

    Workshop findActiveWorkshop(Long id) {
        return workshopRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop", id));
    }
}
