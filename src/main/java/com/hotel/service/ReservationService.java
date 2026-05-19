package com.hotel.service;

import com.hotel.domain.Customer;
import com.hotel.domain.Reservation;
import com.hotel.domain.Room;
import com.hotel.enums.ReservationStatus;
import com.hotel.exception.NotFoundException;
import com.hotel.exception.ReservationConflictException;
import com.hotel.repository.CustomerRepository;
import com.hotel.repository.ReservationRepository;
import com.hotel.repository.RoomRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service that encapsulates reservation-management use-cases.
 *
 * <p>Key business rule enforced here:
 * <pre>
 *   A room cannot have two CONFIRMED reservations whose date ranges overlap.
 *   Overlap: newCheckIn &lt; existingCheckOut AND newCheckOut &gt; existingCheckIn
 * </pre>
 */
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository        roomRepository;
    private final CustomerRepository    customerRepository;

    /**
     * Constructs the service with its required collaborators.
     *
     * @param reservationRepository reservation storage abstraction
     * @param roomRepository        room storage abstraction
     * @param customerRepository    customer storage abstraction
     */
    public ReservationService(ReservationRepository reservationRepository,
                               RoomRepository        roomRepository,
                               CustomerRepository    customerRepository) {

        this.reservationRepository = Objects.requireNonNull(reservationRepository,
                "ReservationRepository must not be null.");
        this.roomRepository        = Objects.requireNonNull(roomRepository,
                "RoomRepository must not be null.");
        this.customerRepository    = Objects.requireNonNull(customerRepository,
                "CustomerRepository must not be null.");
    }

    // ── Use-cases ──────────────────────────────────────────────────────────────

    /**
     * Creates a new CONFIRMED reservation after validating there is no
     * scheduling conflict for the requested room.
     *
     * @param customerId the guest's id
     * @param roomId     the desired room's id
     * @param checkIn    arrival date (inclusive)
     * @param checkOut   departure date (exclusive, must be after checkIn)
     * @return the persisted {@link Reservation}
     * @throws NotFoundException             if customer or room cannot be found
     * @throws ReservationConflictException  if the room is already booked in that period
     */
    public Reservation createReservation(UUID customerId,
                                          UUID roomId,
                                          LocalDate checkIn,
                                          LocalDate checkOut) {

        Objects.requireNonNull(customerId, "Customer id must not be null.");
        Objects.requireNonNull(roomId,     "Room id must not be null.");
        Objects.requireNonNull(checkIn,    "Check-in date must not be null.");
        Objects.requireNonNull(checkOut,   "Check-out date must not be null.");

        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException(
                    "Check-out date must be strictly after check-in date.");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException(
                        "Customer not found with id: " + customerId));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException(
                        "Room not found with id: " + roomId));

        // ── Conflict guard ──────────────────────────────────────────────────
        boolean hasConflict = reservationRepository.findByRoomId(roomId).stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
                .anyMatch(r -> r.overlapsWith(checkIn, checkOut));

        if (hasConflict) {
            throw new ReservationConflictException(String.format(
                    "Room %s is already booked for a period that overlaps %s – %s.",
                    room.getRoomNumber(), checkIn, checkOut));
        }
        // ───────────────────────────────────────────────────────────────────

        Reservation reservation = new Reservation(
                UUID.randomUUID(),
                customer,
                room,
                checkIn,
                checkOut,
                ReservationStatus.CONFIRMED);

        return reservationRepository.save(reservation);
    }

    /**
     * Cancels an existing reservation.
     *
     * <p>Only {@code CONFIRMED} reservations may be cancelled; attempting
     * to cancel a {@code CANCELLED} or {@code COMPLETED} one is a no-op
     * that still succeeds (idempotent).
     *
     * @param reservationId the reservation to cancel
     * @return the updated {@link Reservation}
     * @throws NotFoundException if no reservation with the given id exists
     */
    public Reservation cancelReservation(UUID reservationId) {
        Reservation reservation = getReservationById(reservationId);
        reservation.setStatus(ReservationStatus.CANCELLED);
        return reservationRepository.save(reservation);
    }

    /**
     * Marks a reservation as completed (guest has checked out).
     *
     * @param reservationId the reservation to complete
     * @return the updated {@link Reservation}
     * @throws NotFoundException if no reservation with the given id exists
     */
    public Reservation completeReservation(UUID reservationId) {
        Reservation reservation = getReservationById(reservationId);
        reservation.setStatus(ReservationStatus.COMPLETED);
        return reservationRepository.save(reservation);
    }

    /**
     * Retrieves all reservations associated with a specific customer.
     *
     * @param customerId the customer's id
     * @return list of reservations (possibly empty)
     */
    public List<Reservation> getReservationsByCustomer(UUID customerId) {
        Objects.requireNonNull(customerId, "Customer id must not be null.");
        return reservationRepository.findByCustomerId(customerId);
    }

    /**
     * Retrieves all reservations whose stay overlaps the given date window.
     *
     * @param from window start (inclusive)
     * @param to   window end (exclusive)
     * @return list of reservations (possibly empty)
     */
    public List<Reservation> getReservationsByDateRange(LocalDate from, LocalDate to) {
        Objects.requireNonNull(from, "From date must not be null.");
        Objects.requireNonNull(to,   "To date must not be null.");
        return reservationRepository.findByDateRange(from, to);
    }

    /**
     * Checks whether a room is free for the entire requested period.
     *
     * @param roomId   the room to check
     * @param checkIn  proposed arrival date
     * @param checkOut proposed departure date
     * @return {@code true} if no confirmed booking overlaps the period
     */
    public boolean isRoomAvailable(UUID roomId, LocalDate checkIn, LocalDate checkOut) {
        return reservationRepository.findByRoomId(roomId).stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
                .noneMatch(r -> r.overlapsWith(checkIn, checkOut));
    }

    /**
     * Returns all reservations in the system.
     *
     * @return all reservations
     */
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private Reservation getReservationById(UUID id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Reservation not found with id: " + id));
    }
}
