package com.hotel.repository;

import com.hotel.db.DatabaseConnection;
import com.hotel.domain.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MySQLCustomerRepository implements CustomerRepository {

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public Customer save(Customer customer) {
        String sql = """
            INSERT INTO customers (id, first_name, last_name, email, phone_number)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                first_name   = VALUES(first_name),
                last_name    = VALUES(last_name),
                email        = VALUES(email),
                phone_number = VALUES(phone_number)
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, customer.getCustomerId().toString());
            ps.setString(2, customer.getFirstName());
            ps.setString(3, customer.getLastName());
            ps.setString(4, customer.getEmail());
            ps.setString(5, customer.getPhoneNumber());
            ps.executeUpdate();
            return customer;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save customer: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Customer> findById(UUID id) {
        String sql = "SELECT * FROM customers WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, id.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find customer: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Customer> findAll() {
        String sql = "SELECT * FROM customers ORDER BY last_name, first_name";
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            List<Customer> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list customers: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        String sql = "SELECT * FROM customers WHERE email = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, email.toLowerCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find customer by email: " + e.getMessage(), e);
        }
    }

    public void delete(UUID id) {
        String sql = "DELETE FROM customers WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, id.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete customer: " + e.getMessage(), e);
        }
    }

    private Customer mapRow(ResultSet rs) throws SQLException {
        return new Customer(
            UUID.fromString(rs.getString("id")),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getString("email"),
            rs.getString("phone_number")
        );
    }
}
