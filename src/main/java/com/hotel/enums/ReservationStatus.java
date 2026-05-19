package com.hotel.enums;

/**
 * Lifecycle status of a hotel reservation.
 */
public enum ReservationStatus {

    /** Reservation has been accepted and is active. */
    CONFIRMED,

    /** Reservation was voided by the guest or the hotel. */
    CANCELLED,

    /** Guest has checked out; stay is finished. */
    COMPLETED;

    /**
     * Returns a human-friendly, fixed-width label for tabular display.
     */
    public String label() {
        return String.format("%-9s", this.name());
    }
}
