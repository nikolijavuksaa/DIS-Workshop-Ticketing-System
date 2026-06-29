package com.dis.workshopticketing.bookingservice.dto;

import com.dis.workshopticketing.bookingservice.model.Booking;
import com.dis.workshopticketing.bookingservice.model.BookingStatus;

import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        Long userId,
        Long workshopSessionId,
        Long reservationId,
        BookingStatus status,
        LocalDateTime reservationExpiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getUserId(),
                booking.getWorkshopSessionId(),
                booking.getReservationId(),
                booking.getStatus(),
                booking.getReservationExpiresAt(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }
}
