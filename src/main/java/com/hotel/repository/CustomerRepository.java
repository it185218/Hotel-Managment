package com.hotel.repository;

import com.hotel.domain.Customer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository contract for {@link Customer} persistence.
 */
public interface CustomerRepository {

    /**
     * Persists a new customer or replaces an existing one with the same id.
     *
     * @param customer the customer to store (never {@code null})
     * @return the stored customer
     */
    Customer save(Customer customer);

    /**
     * Looks up a customer by their surrogate key.
     *
     * @param id the customer identifier
     * @return an {@link Optional} containing the customer, or empty if not found
     */
    Optional<Customer> findById(UUID id);

    /**
     * Returns every customer in the repository.
     *
     * @return unmodifiable snapshot of all customers
     */
    List<Customer> findAll();

    /**
     * Looks up a customer by their e-mail address (case-insensitive).
     *
     * @param email the e-mail to search for
     * @return an {@link Optional} containing the customer, or empty if not found
     */
    Optional<Customer> findByEmail(String email);
}
