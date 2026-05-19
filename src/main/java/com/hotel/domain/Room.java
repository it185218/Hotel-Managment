package com.hotel.domain;

import com.hotel.enums.RoomStatus;
import com.hotel.enums.RoomType;

import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root representing a physical hotel room.
 *
 * <p>Instances are created via the all-args constructor and mutated only
 * through the explicit {@link #setStatus(RoomStatus)} setter, ensuring
 * the entity is never left in an inconsistent state.
 */
public class Room {

    private final UUID       id;
    private final String     roomNumber;
    private final RoomType   type;
    private final double     pricePerNight;
    private       RoomStatus status;

    /**
     * Constructs a fully initialised {@code Room}.
     *
     * @param id            unique surrogate identifier (never {@code null})
     * @param roomNumber    human-readable room label (e.g. "101")
     * @param type          room category
     * @param pricePerNight nightly rate (must be &gt; 0)
     * @param status        initial operational status
     */
    public Room(UUID id,
                String roomNumber,
                RoomType type,
                double pricePerNight,
                RoomStatus status) {

        if (id == null)          throw new IllegalArgumentException("Room id must not be null.");
        if (roomNumber == null || roomNumber.isBlank())
                                 throw new IllegalArgumentException("Room number must not be blank.");
        if (type == null)        throw new IllegalArgumentException("Room type must not be null.");
        if (pricePerNight <= 0)  throw new IllegalArgumentException("Price per night must be positive.");
        if (status == null)      throw new IllegalArgumentException("Room status must not be null.");

        this.id            = id;
        this.roomNumber    = roomNumber.trim();
        this.type          = type;
        this.pricePerNight = pricePerNight;
        this.status        = status;
    }

    // ── Accessors ──────────────────────────────────────────────────────────────

    public UUID getRoomId()          { return id; }
    public String getRoomNumber()    { return roomNumber; }
    public RoomType getType()        { return type; }
    public double getPricePerNight() { return pricePerNight; }
    public RoomStatus getStatus()    { return status; }

    /**
     * Transitions the room to a new operational status.
     *
     * @param status new status (never {@code null})
     */
    public void setStatus(RoomStatus status) {
        if (status == null) throw new IllegalArgumentException("Status must not be null.");
        this.status = status;
    }

    // ── Object overrides ───────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room room)) return false;
        return Objects.equals(id, room.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Room{number='%s', type=%s, price=%.2f, status=%s}",
                roomNumber, type, pricePerNight, status);
    }
}
