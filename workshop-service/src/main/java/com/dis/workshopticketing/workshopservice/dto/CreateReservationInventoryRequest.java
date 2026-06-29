package com.dis.workshopticketing.workshopservice.dto;

public record CreateReservationInventoryRequest(
        Long workshopSessionId,
        Integer totalCapacity
) {
}
