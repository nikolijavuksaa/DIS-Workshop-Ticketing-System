package com.dis.workshopticketing.workshopservice.dto;

import com.dis.workshopticketing.workshopservice.model.WorkshopSession;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WorkshopSessionResponse(
        Long id,
        Long workshopId,
        String workshopTitle,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String location,
        BigDecimal price,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static WorkshopSessionResponse from(WorkshopSession session) {
        return new WorkshopSessionResponse(
                session.getId(),
                session.getWorkshop().getId(),
                session.getWorkshop().getTitle(),
                session.getStartsAt(),
                session.getEndsAt(),
                session.getLocation(),
                session.getPrice(),
                session.isActive(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
