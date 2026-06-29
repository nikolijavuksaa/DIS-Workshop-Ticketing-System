package com.dis.workshopticketing.notificationservice.dto;

import com.dis.workshopticketing.notificationservice.model.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        Long userId,
        String eventType,
        String title,
        String message,
        Long reservationId,
        Long bookingId,
        Long workshopSessionId,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getEventType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReservationId(),
                notification.getBookingId(),
                notification.getWorkshopSessionId(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt(),
                notification.getUpdatedAt()
        );
    }
}
