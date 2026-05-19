package com.hotel.domain;

import com.hotel.enums.ReservationStatus;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root representing a hotel reservation.
 *
 * <p>The overlap-detection logic lives here so the domain rule is
 * co-located with the data it protects (Single Responsibility at the
 * entity level).
 *
 * <p>Overlap definition (half-open interval [checkIn, checkOut)):
 * <pre>
 *   newCheckIn  &lt; existingCheckOut
 *   AND
 *   newCheckOut &gt; existingCheckIn
 * </pre>
 */
public class Reservation {

    private final UUID              id;
    private final Customer          customer;
    private final Room              room;
    private final LocalDate         checkInDate;
    private final LocalDate         checkOutDate;
    private       ReservationStatus status;

    /**
     * Constructs a fully initialised {@code Reservation}.
     *
     * @param id           unique surrogate identifier (never {@code null})
     * @param customer     the guest making the reservation (never {@code null})
     * @param room         the booked room (never {@code null})
     * @param checkInDate  inclusive arrival date (never {@code null})
     * @param checkOutDate exclusive departure date (must be after {@code checkInDate})
     * @param status       initial lifecycle status
     */
    public Reservation(UUID id,
                       Customer customer,
                       Room room,
                       LocalDate checkInDate,
                       LocalDate checkOutDate,
                       ReservationStatus status) {

        if (id == null)           throw new IllegalArgumentException("Reservation id must not be null.");
        if (customer == null)     throw new IllegalArgumentException("Customer must not be null.");
        if (room == null)         throw new IllegalArgumentException("Room must not be null.");
        if (checkInDate == null)  throw new IllegalArgumentException("Check-in date must not be null.");
        if (checkOutDate == null) throw new IllegalArgumentException("Check-out date must not be null.");
        if (!checkOutDate.isAfter(checkInDate))
                                  throw new IllegalArgumentException(
                                          "Check-out date must be strictly after check-in date.");
        if (status == null)       throw new IllegalArgumentException("Status must not be null.");

        this.id           = id;
        this.customer     = customer;
        this.room         = room;
        this.checkInDate  = checkInDate;
        this.checkOutDate = checkOutDate;
        this.status       = status;
    }

    // ── Accessors ──────────────────────────────────────────────────────────────

    public UUID              getReservationId() { return id; }
    public Customer          getCustomer()      { return customer; }
    public Room              getRoom()          { return room; }
    public LocalDate         getCheckInDate()   { return checkInDate; }
    public LocalDate         getCheckOutDate()  { return checkOutDate; }
    public ReservationStatus getStatus()        { return status; }

    /**
     * Transitions the reservation to a new lifecycle status.
     *
     * @param status new status (never {@code null})
     */
    public void setStatus(ReservationStatus status) {
        if (status == null) throw new IllegalArgumentException("Status must not be null.");
        this.status = status;
    }

    // ── Domain logic ──────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the supplied date range overlaps with this
     * reservation's period using half-open interval semantics.
     *
     * <p>Two stays overlap when:
     * <pre>
     *   newCheckIn  &lt; this.checkOutDate
     *   AND
     *   newCheckOut &gt; this.checkInDate
     * </pre>
     *
     * @param newCheckIn  proposed arrival date (inclusive)
     * @param newCheckOut proposed departure date (exclusive)
     * @return {@code true} when the intervals overlap
     */
    public boolean overlapsWith(LocalDate newCheckIn, LocalDate newCheckOut) {
        return newCheckIn.isBefore(this.checkOutDate)
                && newCheckOut.isAfter(this.checkInDate);
    }

    // ── Object overrides ───────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reservation that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format(
                "Reservation{id=%s, customer='%s', room='%s', %s → %s, status=%s}",
                id,
                customer.getFullName(),
                room.getRoomNumber(),
                checkInDate,
                checkOutDate,
                status);
    }
}
