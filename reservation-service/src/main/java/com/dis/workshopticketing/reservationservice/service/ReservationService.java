package com.dis.workshopticketing.reservationservice.service;

import com.dis.workshopticketing.reservationservice.dto.AvailabilityResponse;
import com.dis.workshopticketing.reservationservice.dto.CreateHoldRequest;
import com.dis.workshopticketing.reservationservice.dto.CreateInventoryRequest;
import com.dis.workshopticketing.reservationservice.dto.ExpirationResponse;
import com.dis.workshopticketing.reservationservice.dto.InventoryResponse;
import com.dis.workshopticketing.reservationservice.dto.ReservationResponse;
import com.dis.workshopticketing.reservationservice.dto.UpdateInventoryCapacityRequest;
import com.dis.workshopticketing.reservationservice.exception.BadRequestException;
import com.dis.workshopticketing.reservationservice.exception.DuplicateInventoryException;
import com.dis.workshopticketing.reservationservice.exception.DuplicateReservationException;
import com.dis.workshopticketing.reservationservice.exception.ResourceNotFoundException;
import com.dis.workshopticketing.reservationservice.messaging.ReservationEventPublisher;
import com.dis.workshopticketing.reservationservice.model.Reservation;
import com.dis.workshopticketing.reservationservice.model.ReservationStatus;
import com.dis.workshopticketing.reservationservice.model.WorkshopSessionInventory;
import com.dis.workshopticketing.reservationservice.repository.ReservationRepository;
import com.dis.workshopticketing.reservationservice.repository.WorkshopSessionInventoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ReservationService {

    private final WorkshopSessionInventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final long holdDurationMinutes;

    public ReservationService(
            WorkshopSessionInventoryRepository inventoryRepository,
            ReservationRepository reservationRepository,
            ReservationEventPublisher eventPublisher,
            PlatformTransactionManager transactionManager,
            Clock clock,
            @Value("${reservation.hold-duration-minutes:10}") long holdDurationMinutes
    ) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
        this.holdDurationMinutes = holdDurationMinutes;
    }

    @Transactional
    public InventoryResponse createInventory(CreateInventoryRequest request) {
        if (inventoryRepository.existsByWorkshopSessionId(request.workshopSessionId())) {
            throw new DuplicateInventoryException(request.workshopSessionId());
        }

        WorkshopSessionInventory inventory = WorkshopSessionInventory.builder()
                .workshopSessionId(request.workshopSessionId())
                .totalCapacity(request.totalCapacity())
                .heldCount(0)
                .confirmedCount(0)
                .waitlistedCount(0)
                .build();

        return InventoryResponse.from(inventoryRepository.saveAndFlush(inventory));
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(Long workshopSessionId) {
        return AvailabilityResponse.from(findInventory(workshopSessionId));
    }

    @Transactional
    public InventoryResponse updateCapacity(Long workshopSessionId, UpdateInventoryCapacityRequest request) {
        WorkshopSessionInventory inventory = findInventory(workshopSessionId);
        int occupiedCount = inventory.getHeldCount() + inventory.getConfirmedCount();

        if (request.totalCapacity() < occupiedCount) {
            throw new BadRequestException("totalCapacity cannot be lower than heldCount + confirmedCount");
        }

        inventory.setTotalCapacity(request.totalCapacity());
        return InventoryResponse.from(inventoryRepository.saveAndFlush(inventory));
    }

    public ReservationResponse createHold(CreateHoldRequest request) {
        try {
            return Objects.requireNonNull(transactionTemplate.execute(status -> createHoldInTransaction(request)));
        } catch (ObjectOptimisticLockingFailureException exception) {
            return Objects.requireNonNull(transactionTemplate.execute(status -> createHoldAfterConflict(request)));
        }
    }

    @Transactional(readOnly = true)
    public ReservationResponse get(Long id) {
        return ReservationResponse.from(findReservation(id));
    }

    @Transactional(readOnly = true)
    public ReservationResponse getByBooking(Long bookingId) {
        return ReservationResponse.from(reservationRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation for booking", bookingId)));
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getAllBySession(Long workshopSessionId) {
        return reservationRepository.findAllByWorkshopSessionId(workshopSessionId)
                .stream()
                .map(ReservationResponse::from)
                .toList();
    }

    @Transactional
    public ReservationResponse confirm(Long id) {
        Reservation reservation = findReservation(id);
        if (reservation.getStatus() != ReservationStatus.HELD) {
            throw new BadRequestException("Only HELD reservations can be confirmed");
        }

        WorkshopSessionInventory inventory = findInventory(reservation.getWorkshopSessionId());
        decrementHeld(inventory);
        inventory.setConfirmedCount(inventory.getConfirmedCount() + 1);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setExpiresAt(null);

        inventoryRepository.saveAndFlush(inventory);
        Reservation savedReservation = reservationRepository.saveAndFlush(reservation);
        eventPublisher.record(ReservationEventPublisher.RESERVATION_CONFIRMED, savedReservation);
        return ReservationResponse.from(savedReservation);
    }

    @Transactional
    public ReservationResponse release(Long id) {
        Reservation reservation = findReservation(id);
        WorkshopSessionInventory inventory = findInventory(reservation.getWorkshopSessionId());
        Reservation promotedReservation = null;

        if (reservation.getStatus() == ReservationStatus.HELD) {
            decrementHeld(inventory);
            reservation.setStatus(ReservationStatus.RELEASED);
            reservation.setExpiresAt(null);
            promotedReservation = promoteFirstWaitlisted(inventory);
        } else if (reservation.getStatus() == ReservationStatus.WAITLISTED) {
            decrementWaitlisted(inventory);
            reservation.setStatus(ReservationStatus.RELEASED);
            reservation.setExpiresAt(null);
        } else if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            decrementConfirmed(inventory);
            reservation.setStatus(ReservationStatus.RELEASED);
            reservation.setExpiresAt(null);
            promotedReservation = promoteFirstWaitlisted(inventory);
        } else {
            throw new BadRequestException("Only HELD, WAITLISTED or CONFIRMED reservations can be released");
        }

        inventoryRepository.saveAndFlush(inventory);
        Reservation savedReservation = reservationRepository.saveAndFlush(reservation);
        eventPublisher.record(ReservationEventPublisher.RESERVATION_RELEASED, savedReservation);
        if (promotedReservation != null) {
            eventPublisher.record(ReservationEventPublisher.WAITLIST_PROMOTED, promotedReservation);
        }

        return ReservationResponse.from(savedReservation);
    }

    @Transactional
    public ExpirationResponse expireHolds() {
        LocalDateTime now = now();
        List<Reservation> expiredReservations = reservationRepository.findAllByStatusAndExpiresAtBefore(
                ReservationStatus.HELD,
                now
        );
        List<Reservation> promotedReservations = new ArrayList<>();

        for (Reservation reservation : expiredReservations) {
            WorkshopSessionInventory inventory = findInventory(reservation.getWorkshopSessionId());
            decrementHeld(inventory);
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservation.setExpiresAt(null);

            Reservation promotedReservation = promoteFirstWaitlisted(inventory);
            if (promotedReservation != null) {
                promotedReservations.add(promotedReservation);
            }

            inventoryRepository.saveAndFlush(inventory);
            Reservation savedReservation = reservationRepository.saveAndFlush(reservation);
            eventPublisher.record(ReservationEventPublisher.RESERVATION_EXPIRED, savedReservation);
            if (promotedReservation != null) {
                eventPublisher.record(ReservationEventPublisher.WAITLIST_PROMOTED, promotedReservation);
            }
        }

        return new ExpirationResponse(
                expiredReservations.size(),
                expiredReservations.stream().map(ReservationResponse::from).toList(),
                promotedReservations.stream().map(ReservationResponse::from).toList()
        );
    }

    private ReservationResponse createHoldInTransaction(CreateHoldRequest request) {
        ReservationResponse existingReservation = getExistingReservationForBooking(request);
        if (existingReservation != null) {
            return existingReservation;
        }

        WorkshopSessionInventory inventory = findInventory(request.workshopSessionId());

        if (inventory.hasAvailableSeat()) {
            Reservation reservation = createHeldReservation(request, inventory);
            eventPublisher.record(ReservationEventPublisher.RESERVATION_HELD, reservation);
            return ReservationResponse.from(reservation);
        }

        Reservation reservation = createWaitlistedReservation(request, inventory);
        eventPublisher.record(ReservationEventPublisher.RESERVATION_WAITLISTED, reservation);
        return ReservationResponse.from(reservation);
    }

    private ReservationResponse createHoldAfterConflict(CreateHoldRequest request) {
        ReservationResponse existingReservation = getExistingReservationForBooking(request);
        if (existingReservation != null) {
            return existingReservation;
        }

        WorkshopSessionInventory inventory = findInventory(request.workshopSessionId());

        if (inventory.hasAvailableSeat()) {
            Reservation reservation = createHeldReservation(request, inventory);
            eventPublisher.record(ReservationEventPublisher.RESERVATION_HELD, reservation);
            return ReservationResponse.from(reservation);
        }

        Reservation reservation = createWaitlistedReservation(request, inventory);
        eventPublisher.record(ReservationEventPublisher.RESERVATION_WAITLISTED, reservation);
        return ReservationResponse.from(reservation);
    }

    private Reservation createHeldReservation(CreateHoldRequest request, WorkshopSessionInventory inventory) {
        inventory.setHeldCount(inventory.getHeldCount() + 1);
        inventoryRepository.saveAndFlush(inventory);

        Reservation reservation = Reservation.builder()
                .bookingId(request.bookingId())
                .userId(request.userId())
                .workshopSessionId(request.workshopSessionId())
                .status(ReservationStatus.HELD)
                .expiresAt(now().plusMinutes(holdDurationMinutes))
                .build();

        return reservationRepository.saveAndFlush(reservation);
    }

    private Reservation createWaitlistedReservation(CreateHoldRequest request, WorkshopSessionInventory inventory) {
        inventory.setWaitlistedCount(inventory.getWaitlistedCount() + 1);
        inventoryRepository.saveAndFlush(inventory);

        Reservation reservation = Reservation.builder()
                .bookingId(request.bookingId())
                .userId(request.userId())
                .workshopSessionId(request.workshopSessionId())
                .status(ReservationStatus.WAITLISTED)
                .build();

        return reservationRepository.saveAndFlush(reservation);
    }

    private Reservation promoteFirstWaitlisted(WorkshopSessionInventory inventory) {
        return reservationRepository.findFirstByWorkshopSessionIdAndStatusOrderByCreatedAtAsc(
                        inventory.getWorkshopSessionId(),
                        ReservationStatus.WAITLISTED
                )
                .map(reservation -> {
                    decrementWaitlisted(inventory);
                    inventory.setHeldCount(inventory.getHeldCount() + 1);
                    reservation.setStatus(ReservationStatus.HELD);
                    reservation.setExpiresAt(now().plusMinutes(holdDurationMinutes));
                    return reservationRepository.saveAndFlush(reservation);
                })
                .orElse(null);
    }

    private WorkshopSessionInventory findInventory(Long workshopSessionId) {
        return inventoryRepository.findByWorkshopSessionId(workshopSessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop session inventory", workshopSessionId));
    }

    private Reservation findReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", id));
    }

    private ReservationResponse getExistingReservationForBooking(CreateHoldRequest request) {
        return reservationRepository.findByBookingId(request.bookingId())
                .map(reservation -> {
                    if (!reservation.getWorkshopSessionId().equals(request.workshopSessionId())) {
                        throw new DuplicateReservationException(request.bookingId());
                    }
                    return ReservationResponse.from(reservation);
                })
                .orElse(null);
    }

    private void decrementHeld(WorkshopSessionInventory inventory) {
        if (inventory.getHeldCount() <= 0) {
            throw new BadRequestException("Inventory held count cannot become negative");
        }
        inventory.setHeldCount(inventory.getHeldCount() - 1);
    }

    private void decrementConfirmed(WorkshopSessionInventory inventory) {
        if (inventory.getConfirmedCount() <= 0) {
            throw new BadRequestException("Inventory confirmed count cannot become negative");
        }
        inventory.setConfirmedCount(inventory.getConfirmedCount() - 1);
    }

    private void decrementWaitlisted(WorkshopSessionInventory inventory) {
        if (inventory.getWaitlistedCount() <= 0) {
            throw new BadRequestException("Inventory waitlisted count cannot become negative");
        }
        inventory.setWaitlistedCount(inventory.getWaitlistedCount() - 1);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
