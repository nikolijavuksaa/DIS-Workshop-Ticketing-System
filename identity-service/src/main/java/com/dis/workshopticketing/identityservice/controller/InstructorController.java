package com.dis.workshopticketing.identityservice.controller;

import com.dis.workshopticketing.identityservice.dto.CreateInstructorRequest;
import com.dis.workshopticketing.identityservice.dto.ExistenceResponse;
import com.dis.workshopticketing.identityservice.dto.InstructorResponse;
import com.dis.workshopticketing.identityservice.dto.UpdateInstructorRequest;
import com.dis.workshopticketing.identityservice.service.InstructorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/instructors")
public class InstructorController {

    private final InstructorService instructorService;

    public InstructorController(InstructorService instructorService) {
        this.instructorService = instructorService;
    }

    @PostMapping
    public ResponseEntity<InstructorResponse> create(@Valid @RequestBody CreateInstructorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(instructorService.create(request));
    }

    @GetMapping
    public List<InstructorResponse> getAll() {
        return instructorService.getAll();
    }

    @GetMapping("/{id}")
    public InstructorResponse get(@PathVariable Long id) {
        return instructorService.get(id);
    }

    @PutMapping("/{id}")
    public InstructorResponse update(@PathVariable Long id, @Valid @RequestBody UpdateInstructorRequest request) {
        return instructorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        instructorService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/exists")
    public ExistenceResponse exists(@PathVariable Long id) {
        return instructorService.exists(id);
    }
}
