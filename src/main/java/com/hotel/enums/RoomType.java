package com.hotel.enums;

/**
 * Classifies the physical configuration of a hotel room.
 */
public enum RoomType {

    /** Single bed, suited for one guest. */
    SINGLE,

    /** Two beds or one king, suited for two guests. */
    DOUBLE,

    /** Premium multi-room accommodation. */
    SUITE;

    /**
     * Returns a human-friendly, fixed-width label for tabular display.
     */
    public String label() {
        return String.format("%-9s", this.name());
    }
}
