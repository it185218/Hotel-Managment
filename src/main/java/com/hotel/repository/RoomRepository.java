package com.hotel.repository;

import com.hotel.domain.Room;
import com.hotel.enums.RoomStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository contract for {@link Room} persistence.
 *
 * <p>Follows the Repository pattern from Domain-Driven Design: the
 * interface lives in the domain/repository layer and the concrete
 * implementation (in-memory, JPA, etc.) is an infrastructure detail
 * that can be swapped without changing business logic.
 */
public interface RoomRepository {

    /**
     * Persists a new room or replaces an existing one with the same id.
     *
     * @param room the room to store (never {@code null})
     * @return the stored room
     */
    Room save(Room room);

    /**
     * Looks up a room by its surrogate key.
     *
     * @param id the room identifier
     * @return an {@link Optional} containing the room, or empty if not found
     */
    Optional<Room> findById(UUID id);

    /**
     * Returns every room in the repository.
     *
     * @return unmodifiable snapshot of all rooms
     */
    List<Room> findAll();

    /**
     * Returns all rooms whose current status matches the given value.
     *
     * @param status the status filter
     * @return matching rooms (possibly empty)
     */
    List<Room> findByStatus(RoomStatus status);

    /**
     * Looks up a room by its human-readable room number.
     *
     * @param roomNumber the room number (e.g. "101")
     * @return an {@link Optional} containing the room, or empty if not found
     */
    Optional<Room> findByRoomNumber(String roomNumber);
}
