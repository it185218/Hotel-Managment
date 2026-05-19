package com.hotel.repository;

import com.hotel.db.DatabaseConnection;
import com.hotel.domain.Customer;
import com.hotel.domain.Reservation;
import com.hotel.domain.Room;
import com.hotel.enums.ReservationStatus;
import com.hotel.enums.RoomStatus;
import com.hotel.enums.RoomType;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MySQLReservationRepository implements ReservationRepository {

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public Reservation save(Reservation reservation) {
        String sql = """
            INSERT INTO reservations
                (id, customer_id, room_id, check_in_date, check_out_date, status)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                check_in_date  = VALUES(check_in_date),
                check_out_date = VALUES(check_out_date),
                status         = VALUES(status)
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, reservation.getReservationId().toString());
            ps.setString(2, reservation.getCustomer().getCustomerId().toString());
            ps.setString(3, reservation.getRoom().getRoomId().toString());
            ps.setDate(4, Date.valueOf(reservation.getCheckInDate()));
            ps.setDate(5, Date.valueOf(reservation.getCheckOutDate()));
            ps.setString(6, reservation.getStatus().name());
            ps.executeUpdate();
            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save reservation: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Reservation> findById(UUID id) {
        String sql = buildJoinQuery("r.id = ?");
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, id.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservation: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Reservation> findAll() {
        String sql = buildJoinQuery(null) + " ORDER BY r.check_in_date DESC";
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            List<Reservation> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list reservations: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Reservation> findByCustomerId(UUID customerId) {
        String sql = buildJoinQuery("r.customer_id = ?") + " ORDER BY r.check_in_date DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, customerId.toString());
            ResultSet rs = ps.executeQuery();
            List<Reservation> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations by customer: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Reservation> findByDateRange(LocalDate from, LocalDate to) {
        // Overlapping interval: check_in < to AND check_out > from
        String sql = buildJoinQuery("r.check_in_date < ? AND r.check_out_date > ?")
                   + " ORDER BY r.check_in_date";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(to));
            ps.setDate(2, Date.valueOf(from));
            ResultSet rs = ps.executeQuery();
            List<Reservation> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations by date range: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Reservation> findByRoomId(UUID roomId) {
        String sql = buildJoinQuery("r.room_id = ?") + " ORDER BY r.check_in_date";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, roomId.toString());
            ResultSet rs = ps.executeQuery();
            List<Reservation> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations by room: " + e.getMessage(), e);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Builds the full SELECT with JOINs to customers and rooms.
     * @param whereClause optional WHERE clause (without the "WHERE" keyword), or null
     */
    private String buildJoinQuery(String whereClause) {
        String base = """
            SELECT
                r.id            AS res_id,
                r.check_in_date, r.check_out_date,
                r.status        AS res_status,
                c.id            AS cust_id,
                c.first_name, c.last_name, c.email, c.phone_number,
                rm.id           AS room_id,
                rm.room_number, rm.type, rm.price_per_night,
                rm.status       AS room_status
            FROM reservations r
            JOIN customers c  ON r.customer_id = c.id
            JOIN rooms     rm ON r.room_id     = rm.id
            """;
        return whereClause == null ? base : base + " WHERE " + whereClause;
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        Customer customer = new Customer(
            UUID.fromString(rs.getString("cust_id")),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getString("email"),
            rs.getString("phone_number")
        );
        Room room = new Room(
            UUID.fromString(rs.getString("room_id")),
            rs.getString("room_number"),
            RoomType.valueOf(rs.getString("type")),
            rs.getDouble("price_per_night"),
            RoomStatus.valueOf(rs.getString("room_status"))
        );
        return new Reservation(
            UUID.fromString(rs.getString("res_id")),
            customer,
            room,
            rs.getDate("check_in_date").toLocalDate(),
            rs.getDate("check_out_date").toLocalDate(),
            ReservationStatus.valueOf(rs.getString("res_status"))
        );
    }
}
