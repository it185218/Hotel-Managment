package com.hotel.repository;

import com.hotel.domain.Customer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe, in-memory implementation of {@link CustomerRepository}.
 */
public class InMemoryCustomerRepository implements CustomerRepository {

    private final Map<UUID, Customer> store = new ConcurrentHashMap<>();

    // ── CustomerRepository ─────────────────────────────────────────────────────

    @Override
    public Customer save(Customer customer) {
        Objects.requireNonNull(customer, "Customer must not be null.");
        store.put(customer.getCustomerId(), customer);
        return customer;
    }

    @Override
    public Optional<Customer> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Customer> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(store.values()));
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        String normalised = email.trim().toLowerCase();
        return store.values().stream()
                .filter(c -> c.getEmail().equalsIgnoreCase(normalised))
                .findFirst();
    }
}
