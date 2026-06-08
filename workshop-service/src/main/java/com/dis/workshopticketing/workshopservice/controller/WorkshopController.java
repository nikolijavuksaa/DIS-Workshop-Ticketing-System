package com.dis.workshopticketing.workshopservice.controller;

import com.dis.workshopticketing.workshopservice.dto.CreateWorkshopRequest;
import com.dis.workshopticketing.workshopservice.dto.CreateWorkshopSessionRequest;
import com.dis.workshopticketing.workshopservice.dto.UpdateWorkshopRequest;
import com.dis.workshopticketing.workshopservice.dto.WorkshopResponse;
import com.dis.workshopticketing.workshopservice.dto.WorkshopSessionResponse;
import com.dis.workshopticketing.workshopservice.service.WorkshopService;
import com.dis.workshopticketing.workshopservice.service.WorkshopSessionService;
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
@RequestMapping("/workshops")
public class WorkshopController {

    private final WorkshopService workshopService;
    private final WorkshopSessionService workshopSessionService;

    public WorkshopController(WorkshopService workshopService, WorkshopSessionService workshopSessionService) {
        this.workshopService = workshopService;
        this.workshopSessionService = workshopSessionService;
    }

    @PostMapping
    public ResponseEntity<WorkshopResponse> create(@Valid @RequestBody CreateWorkshopRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workshopService.create(request));
    }

    @GetMapping
    public List<WorkshopResponse> getAll() {
        return workshopService.getAll();
    }

    @GetMapping("/{id}")
    public WorkshopResponse get(@PathVariable Long id) {
        return workshopService.get(id);
    }

    @PutMapping("/{id}")
    public WorkshopResponse update(@PathVariable Long id, @Valid @RequestBody UpdateWorkshopRequest request) {
        return workshopService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        workshopService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{workshopId}/sessions")
    public ResponseEntity<WorkshopSessionResponse> createSession(
            @PathVariable Long workshopId,
            @Valid @RequestBody CreateWorkshopSessionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workshopSessionService.create(workshopId, request));
    }

    @GetMapping("/{workshopId}/sessions")
    public List<WorkshopSessionResponse> getSessions(@PathVariable Long workshopId) {
        return workshopSessionService.getAllByWorkshop(workshopId);
    }
}
