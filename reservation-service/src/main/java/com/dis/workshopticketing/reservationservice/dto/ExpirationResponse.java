package com.dis.workshopticketing.reservationservice.dto;

import java.util.List;

public record ExpirationResponse(
        int expiredCount,
        List<ReservationResponse> expiredReservations,
        List<ReservationResponse> promotedReservations
) {
}
