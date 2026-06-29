package com.dis.workshopticketing.reservationservice.dto;

import com.dis.workshopticketing.reservationservice.model.WorkshopSessionInventory;

public record AvailabilityResponse(
        Long workshopSessionId,
        Integer totalCapacity,
        Integer heldCount,
        Integer confirmedCount,
        Integer waitlistedCount,
        Integer availableCount
) {

    public static AvailabilityResponse from(WorkshopSessionInventory inventory) {
        return new AvailabilityResponse(
                inventory.getWorkshopSessionId(),
                inventory.getTotalCapacity(),
                inventory.getHeldCount(),
                inventory.getConfirmedCount(),
                inventory.getWaitlistedCount(),
                inventory.availableCount()
        );
    }
}
