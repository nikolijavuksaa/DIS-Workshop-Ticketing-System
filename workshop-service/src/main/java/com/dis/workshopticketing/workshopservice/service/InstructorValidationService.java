package com.dis.workshopticketing.workshopservice.service;

import com.dis.workshopticketing.workshopservice.client.IdentityInstructorClient;
import com.dis.workshopticketing.workshopservice.dto.ExistenceResponse;
import com.dis.workshopticketing.workshopservice.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class InstructorValidationService {

    private final IdentityInstructorClient identityInstructorClient;

    public InstructorValidationService(IdentityInstructorClient identityInstructorClient) {
        this.identityInstructorClient = identityInstructorClient;
    }

    public void ensureInstructorExists(Long instructorId) {
        ExistenceResponse response = identityInstructorClient.exists(instructorId);

        if (!response.exists()) {
            throw new ResourceNotFoundException("Instructor", instructorId);
        }
    }
}
