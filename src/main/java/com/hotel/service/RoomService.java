package com.hotel.service;

import com.hotel.domain.Room;
import com.hotel.enums.RoomStatus;
import com.hotel.enums.RoomType;
import com.hotel.exception.NotFoundException;
import com.hotel.repository.ReservationRepository;
import com.hotel.repository.RoomRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service that encapsulates room-management use-cases.
 *
 * <p>Delegates persistence to {@link RoomRepository} and queries
 * {@link ReservationRepository} to compute real-time availability.
 * Contains no HTTP/console concerns — those live in {@code Main}.
 */
public class RoomService {

    private final RoomRepository        roomRepository;
    private final ReservationRepository reservationRepository;

    /**
     * Constructs the service with its required collaborators.
     *
     * @param roomRepository        room storage abstraction
     * @param reservationRepository reservation storage abstraction (used for availability)
     */
    public RoomService(RoomRepository roomRepository,
                       ReservationRepository reservationRepository) {
        this.roomRepository        = Objects.requireNonNull(roomRepository,
                "RoomRepository must not be null.");
        this.reservationRepository = Objects.requireNonNull(reservationRepository,
                "ReservationRepository must not be null.");
    }

    // ── Use-cases ──────────────────────────────────────────────────────────────

    /**
     * Creates and persists a new room.
     *
     * @param roomNumber    unique room label
     * @param type          room category
     * @param pricePerNight nightly rate (must be &gt; 0)
     * @return the persisted {@link Room}
     * @throws IllegalArgumentException if any argument is invalid
     */
    public Room createRoom(String roomNumber, RoomType type, double pricePerNight) {
        Room room = new Room(UUID.randomUUID(), roomNumber, type, pricePerNight, RoomStatus.AVAILABLE);
        return roomRepository.save(room);
    }

    /**
     * Returns all rooms currently in {@code AVAILABLE} status,
     * regardless of reservation calendar.
     *
     * @return list of available rooms
     */
    public List<Room> getAvailableRooms() {
        return roomRepository.findByStatus(RoomStatus.AVAILABLE);
    }

    /**
     * Returns all rooms that are both operationally {@code AVAILABLE}
     * and have no confirmed reservation overlapping the requested dates.
     *
     * @param checkIn  desired arrival date (inclusive)
     * @param checkOut desired departure date (exclusive)
     * @return rooms available for the entire date window
     */
    public List<Room> getAvailableRoomsForDateRange(LocalDate checkIn, LocalDate checkOut) {
        Objects.requireNonNull(checkIn,  "Check-in date must not be null.");
        Objects.requireNonNull(checkOut, "Check-out date must not be null.");

        // UUIDs of rooms that already have a confirmed booking overlapping the window
        var bookedRoomIds = reservationRepository.findAll().stream()
                .filter(r -> switch (r.getStatus()) {
                    case CONFIRMED -> r.overlapsWith(checkIn, checkOut);
                    default        -> false;
                })
                .map(r -> r.getRoom().getRoomId())
                .collect(Collectors.toSet());

        return roomRepository.findByStatus(RoomStatus.AVAILABLE).stream()
                .filter(room -> !bookedRoomIds.contains(room.getRoomId()))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Changes the operational status of a room.
     *
     * @param roomId the room to update
     * @param status new status
     * @return the updated {@link Room}
     * @throws NotFoundException if no room with the given id exists
     */
    public Room updateRoomStatus(UUID roomId, RoomStatus status) {
        Room room = getRoomById(roomId);
        room.setStatus(status);
        return roomRepository.save(room);
    }

    /**
     * Retrieves a room by id or throws {@link NotFoundException}.
     *
     * @param roomId the room identifier
     * @return the room
     * @throws NotFoundException if no room with the given id exists
     */
    public Room getRoomById(UUID roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException(
                        "Room not found with id: " + roomId));
    }

    /**
     * Returns all rooms registered in the system.
     *
     * @return all rooms
     */
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }
}
