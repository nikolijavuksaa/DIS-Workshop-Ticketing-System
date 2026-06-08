package com.dis.workshopticketing.workshopservice.controller;

import com.dis.workshopticketing.workshopservice.dto.ExistenceResponse;
import com.dis.workshopticketing.workshopservice.dto.UpdateWorkshopSessionRequest;
import com.dis.workshopticketing.workshopservice.dto.WorkshopSessionResponse;
import com.dis.workshopticketing.workshopservice.service.WorkshopSessionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sessions")
public class WorkshopSessionController {

    private final WorkshopSessionService workshopSessionService;

    public WorkshopSessionController(WorkshopSessionService workshopSessionService) {
        this.workshopSessionService = workshopSessionService;
    }

    @GetMapping
    public List<WorkshopSessionResponse> getAll() {
        return workshopSessionService.getAll();
    }

    @GetMapping("/{id}")
    public WorkshopSessionResponse get(@PathVariable Long id) {
        return workshopSessionService.get(id);
    }

    @PutMapping("/{id}")
    public WorkshopSessionResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkshopSessionRequest request
    ) {
        return workshopSessionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        workshopSessionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/exists")
    public ExistenceResponse exists(@PathVariable Long id) {
        return workshopSessionService.exists(id);
    }
}
