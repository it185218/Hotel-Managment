package com.hotel.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a physical floor of the hotel.
 * Each floor can contain multiple rooms.
 */
public class Floor {

    private final UUID   id;
    private final int    floorNumber;
    private final String description;

    public Floor(UUID id, int floorNumber, String description) {
        if (id == null)         throw new IllegalArgumentException("Floor id must not be null.");
        if (floorNumber < 0)    throw new IllegalArgumentException("Floor number must be >= 0.");
        this.id          = id;
        this.floorNumber = floorNumber;
        this.description = description == null ? "" : description.trim();
    }

    public UUID   getFloorId()     { return id; }
    public int    getFloorNumber() { return floorNumber; }
    public String getDescription() { return description; }

    public String getDisplayName() {
        return description != null && !description.isBlank()
            ? "Floor " + floorNumber + " - " + description
            : "Floor " + floorNumber;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Floor f)) return false;
        return Objects.equals(id, f.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() {
        return "Floor{number=" + floorNumber + ", desc='" + description + "'}";
    }
}
