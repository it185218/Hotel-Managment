package com.hotel.enums;

/**
 * Operational status of a hotel room.
 */
public enum RoomStatus {

    /** Room is clean and ready to accept a new reservation. */
    AVAILABLE,

    /** Room is currently occupied by a guest. */
    OCCUPIED,

    /** Room is out of service (cleaning, repairs, etc.). */
    MAINTENANCE;

    /**
     * Returns a human-friendly, fixed-width label for tabular display.
     */
    public String label() {
        return String.format("%-11s", this.name());
    }
}
