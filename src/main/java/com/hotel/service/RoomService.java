package com.hotel.service;

import com.hotel.domain.Room;
import com.hotel.enums.ReservationStatus;
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

public class RoomService {

    private final RoomRepository        roomRepository;
    private final ReservationRepository reservationRepository;

    public RoomService(RoomRepository roomRepository,
                       ReservationRepository reservationRepository) {
        this.roomRepository        = Objects.requireNonNull(roomRepository);
        this.reservationRepository = Objects.requireNonNull(reservationRepository);
    }

    public Room createRoom(String roomNumber, RoomType type, double pricePerNight) {
        Room room = new Room(UUID.randomUUID(), roomNumber, type, pricePerNight, RoomStatus.AVAILABLE);
        return roomRepository.save(room);
    }

    public List<Room> getAvailableRooms() {
        return roomRepository.findByStatus(RoomStatus.AVAILABLE);
    }

    /**
     * Returns rooms that are:
     * 1. NOT in MAINTENANCE
     * 2. Have NO confirmed reservation overlapping the requested date range
     *
     * This means a room that is currently OCCUPIED can still appear as
     * available if its active booking does NOT overlap the requested dates.
     */
    public List<Room> getAvailableRoomsForDateRange(LocalDate checkIn, LocalDate checkOut) {
        Objects.requireNonNull(checkIn,  "Check-in date must not be null.");
        Objects.requireNonNull(checkOut, "Check-out date must not be null.");

        // Collect IDs of rooms that have a CONFIRMED booking overlapping these dates
        var bookedRoomIds = reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
                .filter(r -> r.overlapsWith(checkIn, checkOut))
                .map(r -> r.getRoom().getRoomId())
                .collect(Collectors.toSet());

        // Return ALL rooms that are not in MAINTENANCE and not in the booked set
        return roomRepository.findAll().stream()
                .filter(room -> room.getStatus() != RoomStatus.MAINTENANCE)
                .filter(room -> !bookedRoomIds.contains(room.getRoomId()))
                .collect(Collectors.toUnmodifiableList());
    }

    public Room updateRoomStatus(UUID roomId, RoomStatus status) {
        Room room = getRoomById(roomId);
        room.setStatus(status);
        return roomRepository.save(room);
    }

    public Room getRoomById(UUID roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room not found with id: " + roomId));
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }
}
