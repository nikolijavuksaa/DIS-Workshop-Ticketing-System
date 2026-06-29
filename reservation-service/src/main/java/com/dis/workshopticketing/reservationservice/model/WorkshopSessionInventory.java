package com.dis.workshopticketing.reservationservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "workshop_session_inventories")
public class WorkshopSessionInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long workshopSessionId;

    @Column(nullable = false)
    private Integer totalCapacity;

    @Builder.Default
    @Column(nullable = false)
    private Integer heldCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer confirmedCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer waitlistedCount = 0;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public int availableCount() {
        return totalCapacity - heldCount - confirmedCount;
    }

    public boolean hasAvailableSeat() {
        return availableCount() > 0;
    }
}
