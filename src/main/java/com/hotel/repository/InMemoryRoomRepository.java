package com.hotel.repository;

import com.hotel.domain.Room;
import com.hotel.enums.RoomStatus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe, in-memory implementation of {@link RoomRepository}.
 *
 * <p>Uses a {@link ConcurrentHashMap} as the backing store so the
 * application can be safely exercised from multiple threads without
 * additional synchronisation in the service layer.
 */
public class InMemoryRoomRepository implements RoomRepository {

    private final Map<UUID, Room> store = new ConcurrentHashMap<>();

    // ── RoomRepository ─────────────────────────────────────────────────────────

    @Override
    public Room save(Room room) {
        Objects.requireNonNull(room, "Room must not be null.");
        store.put(room.getRoomId(), room);
        return room;
    }

    @Override
    public Optional<Room> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Room> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(store.values()));
    }

    @Override
    public List<Room> findByStatus(RoomStatus status) {
        Objects.requireNonNull(status, "Status must not be null.");
        return store.values().stream()
                .filter(r -> r.getStatus() == status)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Optional<Room> findByRoomNumber(String roomNumber) {
        if (roomNumber == null || roomNumber.isBlank()) return Optional.empty();
        return store.values().stream()
                .filter(r -> r.getRoomNumber().equalsIgnoreCase(roomNumber.trim()))
                .findFirst();
    }
}
