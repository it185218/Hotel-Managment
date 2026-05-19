package com.hotel.exception;

/**
 * Thrown when a requested entity cannot be found in the repository.
 *
 * <p>This is an unchecked exception; callers are not forced to handle it
 * unless they have a meaningful recovery strategy.
 */
public class NotFoundException extends RuntimeException {

    /**
     * Constructs a {@code NotFoundException} with a descriptive message.
     *
     * @param message human-readable description of what was not found
     */
    public NotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code NotFoundException} with a message and a root cause.
     *
     * @param message human-readable description
     * @param cause   underlying cause
     */
    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
