package com.hotel.repository;

import com.hotel.domain.Reservation;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe, in-memory implementation of {@link ReservationRepository}.
 */
public class InMemoryReservationRepository implements ReservationRepository {

    private final Map<UUID, Reservation> store = new ConcurrentHashMap<>();

    // ── ReservationRepository ──────────────────────────────────────────────────

    @Override
    public Reservation save(Reservation reservation) {
        Objects.requireNonNull(reservation, "Reservation must not be null.");
        store.put(reservation.getReservationId(), reservation);
        return reservation;
    }

    @Override
    public Optional<Reservation> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Reservation> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(store.values()));
    }

    @Override
    public List<Reservation> findByCustomerId(UUID customerId) {
        Objects.requireNonNull(customerId, "Customer id must not be null.");
        return store.values().stream()
                .filter(r -> r.getCustomer().getCustomerId().equals(customerId))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Returns reservations whose stay period overlaps the supplied window.
     *
     * <p>Overlap condition (half-open intervals):
     * {@code reservation.checkIn < to  &&  reservation.checkOut > from}
     */
    @Override
    public List<Reservation> findByDateRange(LocalDate from, LocalDate to) {
        Objects.requireNonNull(from, "From date must not be null.");
        Objects.requireNonNull(to,   "To date must not be null.");
        return store.values().stream()
                .filter(r -> r.getCheckInDate().isBefore(to)
                          && r.getCheckOutDate().isAfter(from))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<Reservation> findByRoomId(UUID roomId) {
        Objects.requireNonNull(roomId, "Room id must not be null.");
        return store.values().stream()
                .filter(r -> r.getRoom().getRoomId().equals(roomId))
                .collect(Collectors.toUnmodifiableList());
    }
}
