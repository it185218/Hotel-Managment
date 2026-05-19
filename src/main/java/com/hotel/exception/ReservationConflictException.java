package com.hotel.exception;

/**
 * Thrown when a new reservation request overlaps with an existing,
 * active reservation for the same room.
 *
 * <p>This signals a business-rule violation, not a programming error.
 * Callers should present a user-friendly message derived from
 * {@link #getMessage()}.
 */
public class ReservationConflictException extends RuntimeException {

    /**
     * Constructs a {@code ReservationConflictException} with a descriptive message.
     *
     * @param message explanation of the scheduling conflict
     */
    public ReservationConflictException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code ReservationConflictException} with a message and root cause.
     *
     * @param message explanation of the scheduling conflict
     * @param cause   underlying cause
     */
    public ReservationConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
