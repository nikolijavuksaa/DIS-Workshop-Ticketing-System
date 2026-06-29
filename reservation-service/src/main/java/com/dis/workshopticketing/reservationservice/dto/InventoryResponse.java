package com.dis.workshopticketing.reservationservice.dto;

import com.dis.workshopticketing.reservationservice.model.WorkshopSessionInventory;

import java.time.LocalDateTime;

public record InventoryResponse(
        Long id,
        Long workshopSessionId,
        Integer totalCapacity,
        Integer heldCount,
        Integer confirmedCount,
        Integer waitlistedCount,
        Integer availableCount,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static InventoryResponse from(WorkshopSessionInventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getWorkshopSessionId(),
                inventory.getTotalCapacity(),
                inventory.getHeldCount(),
                inventory.getConfirmedCount(),
                inventory.getWaitlistedCount(),
                inventory.availableCount(),
                inventory.getVersion(),
                inventory.getCreatedAt(),
                inventory.getUpdatedAt()
        );
    }
}
