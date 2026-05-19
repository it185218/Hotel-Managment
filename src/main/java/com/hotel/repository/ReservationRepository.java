package com.hotel.repository;

import com.hotel.domain.Reservation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository contract for {@link Reservation} persistence.
 */
public interface ReservationRepository {

    /**
     * Persists a new reservation or replaces an existing one with the same id.
     *
     * @param reservation the reservation to store (never {@code null})
     * @return the stored reservation
     */
    Reservation save(Reservation reservation);

    /**
     * Looks up a reservation by its surrogate key.
     *
     * @param id the reservation identifier
     * @return an {@link Optional} containing the reservation, or empty if not found
     */
    Optional<Reservation> findById(UUID id);

    /**
     * Returns every reservation in the repository.
     *
     * @return unmodifiable snapshot of all reservations
     */
    List<Reservation> findAll();

    /**
     * Returns all reservations belonging to a specific customer.
     *
     * @param customerId the customer's surrogate key
     * @return matching reservations (possibly empty)
     */
    List<Reservation> findByCustomerId(UUID customerId);

    /**
     * Returns all reservations whose stay period overlaps the given date window.
     *
     * <p>Uses half-open interval semantics:
     * {@code reservation.checkIn < to && reservation.checkOut > from}.
     *
     * @param from window start (inclusive)
     * @param to   window end (exclusive)
     * @return matching reservations (possibly empty)
     */
    List<Reservation> findByDateRange(LocalDate from, LocalDate to);

    /**
     * Returns all reservations for a specific room.
     *
     * @param roomId the room's surrogate key
     * @return matching reservations (possibly empty)
     */
    List<Reservation> findByRoomId(UUID roomId);
}
