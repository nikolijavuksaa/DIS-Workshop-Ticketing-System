package com.dis.workshopticketing.identityservice.service;

import com.dis.workshopticketing.identityservice.dto.CreateInstructorRequest;
import com.dis.workshopticketing.identityservice.dto.ExistenceResponse;
import com.dis.workshopticketing.identityservice.dto.InstructorResponse;
import com.dis.workshopticketing.identityservice.dto.UpdateInstructorRequest;
import com.dis.workshopticketing.identityservice.exception.ResourceNotFoundException;
import com.dis.workshopticketing.identityservice.model.IdentityEmailOwnerType;
import com.dis.workshopticketing.identityservice.model.Instructor;
import com.dis.workshopticketing.identityservice.repository.InstructorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InstructorService {

    private final InstructorRepository instructorRepository;
    private final EmailReservationService emailReservationService;

    public InstructorService(InstructorRepository instructorRepository, EmailReservationService emailReservationService) {
        this.instructorRepository = instructorRepository;
        this.emailReservationService = emailReservationService;
    }

    @Transactional
    public InstructorResponse create(CreateInstructorRequest request) {
        String email = emailReservationService.normalize(request.email());
        Instructor instructor = Instructor.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(email)
                .bio(request.bio())
                .expertise(request.expertise())
                .active(true)
                .build();
        Instructor savedInstructor = instructorRepository.saveAndFlush(instructor);

        emailReservationService.reserve(email, IdentityEmailOwnerType.INSTRUCTOR, savedInstructor.getId());

        return InstructorResponse.from(savedInstructor);
    }

    @Transactional(readOnly = true)
    public List<InstructorResponse> getAll() {
        return instructorRepository.findAll()
                .stream()
                .map(InstructorResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public InstructorResponse get(Long id) {
        return InstructorResponse.from(findActiveInstructor(id));
    }

    @Transactional
    public InstructorResponse update(Long id, UpdateInstructorRequest request) {
        Instructor instructor = findActiveInstructor(id);
        String email = emailReservationService.normalize(request.email());

        if (!email.equals(instructor.getEmail())) {
            emailReservationService.reserve(email, IdentityEmailOwnerType.INSTRUCTOR, instructor.getId());
            instructor.setEmail(email);
        }

        instructor.setFirstName(request.firstName());
        instructor.setLastName(request.lastName());
        instructor.setBio(request.bio());
        instructor.setExpertise(request.expertise());

        return InstructorResponse.from(instructorRepository.saveAndFlush(instructor));
    }

    @Transactional
    public void delete(Long id) {
        Instructor instructor = findActiveInstructor(id);
        instructor.setActive(false);
    }

    @Transactional(readOnly = true)
    public ExistenceResponse exists(Long id) {
        return new ExistenceResponse(id, instructorRepository.existsByIdAndActiveTrue(id));
    }

    private Instructor findActiveInstructor(Long id) {
        return instructorRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instructor", id));
    }
}
