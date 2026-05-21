package com.hotel.domain;

import com.hotel.enums.RoomStatus;
import com.hotel.enums.RoomType;

import java.util.Objects;
import java.util.UUID;

public class Room {

    private final UUID       id;
    private final String     roomNumber;
    private final RoomType   type;
    private final double     pricePerNight;
    private       RoomStatus status;
    private       Floor      floor;   // nullable — room may not be assigned to a floor yet

    public Room(UUID id, String roomNumber, RoomType type,
                double pricePerNight, RoomStatus status, Floor floor) {
        if (id == null)         throw new IllegalArgumentException("Room id must not be null.");
        if (roomNumber == null || roomNumber.isBlank())
                                throw new IllegalArgumentException("Room number must not be blank.");
        if (type == null)       throw new IllegalArgumentException("Room type must not be null.");
        if (pricePerNight <= 0) throw new IllegalArgumentException("Price per night must be positive.");
        if (status == null)     throw new IllegalArgumentException("Room status must not be null.");

        this.id            = id;
        this.roomNumber    = roomNumber.trim();
        this.type          = type;
        this.pricePerNight = pricePerNight;
        this.status        = status;
        this.floor         = floor;
    }

    // Backward-compatible constructor without floor
    public Room(UUID id, String roomNumber, RoomType type,
                double pricePerNight, RoomStatus status) {
        this(id, roomNumber, type, pricePerNight, status, null);
    }

    public UUID      getRoomId()          { return id; }
    public String    getRoomNumber()      { return roomNumber; }
    public RoomType  getType()            { return type; }
    public double    getPricePerNight()   { return pricePerNight; }
    public RoomStatus getStatus()         { return status; }
    public Floor     getFloor()           { return floor; }

    public void setStatus(RoomStatus status) {
        if (status == null) throw new IllegalArgumentException("Status must not be null.");
        this.status = status;
    }

    public void setFloor(Floor floor) { this.floor = floor; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room r)) return false;
        return Objects.equals(id, r.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() {
        return String.format("Room{number='%s', type=%s, price=%.2f, status=%s, floor=%s}",
            roomNumber, type, pricePerNight, status,
            floor != null ? floor.getFloorNumber() : "unassigned");
    }
}
