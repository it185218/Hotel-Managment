package com.hotel.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a hotel guest (customer).
 *
 * <p>All fields except {@code phoneNumber} are mandatory. The entity is
 * effectively immutable after construction — mutability is intentionally
 * omitted to keep the domain model simple for this academic project.
 */
public class Customer {

    private final UUID   id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phoneNumber;

    /**
     * Constructs a fully initialised {@code Customer}.
     *
     * @param id          unique surrogate identifier (never {@code null})
     * @param firstName   given name (non-blank)
     * @param lastName    family name (non-blank)
     * @param email       contact e-mail (non-blank; uniqueness enforced at repository level)
     * @param phoneNumber optional contact number (may be {@code null} or blank)
     */
    public Customer(UUID id,
                    String firstName,
                    String lastName,
                    String email,
                    String phoneNumber) {

        if (id == null)           throw new IllegalArgumentException("Customer id must not be null.");
        if (firstName == null || firstName.isBlank())
                                  throw new IllegalArgumentException("First name must not be blank.");
        if (lastName == null || lastName.isBlank())
                                  throw new IllegalArgumentException("Last name must not be blank.");
        if (email == null || email.isBlank())
                                  throw new IllegalArgumentException("Email must not be blank.");

        this.id          = id;
        this.firstName   = firstName.trim();
        this.lastName    = lastName.trim();
        this.email       = email.trim().toLowerCase();
        this.phoneNumber = (phoneNumber == null) ? "" : phoneNumber.trim();
    }

    // ── Accessors ──────────────────────────────────────────────────────────────

    public UUID   getCustomerId()  { return id; }
    public String getFirstName()   { return firstName; }
    public String getLastName()    { return lastName; }
    public String getEmail()       { return email; }
    public String getPhoneNumber() { return phoneNumber; }

    /** Convenience method: returns "FirstName LastName". */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    // ── Object overrides ───────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer customer)) return false;
        return Objects.equals(id, customer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Customer{name='%s %s', email='%s', phone='%s'}",
                firstName, lastName, email, phoneNumber);
    }
}
