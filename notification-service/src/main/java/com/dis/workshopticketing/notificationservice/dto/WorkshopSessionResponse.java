package com.dis.workshopticketing.notificationservice.dto;

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
        Integer capacity,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
