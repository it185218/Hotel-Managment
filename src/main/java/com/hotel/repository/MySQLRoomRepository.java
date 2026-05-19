package com.hotel.repository;

import com.hotel.db.DatabaseConnection;
import com.hotel.domain.Room;
import com.hotel.enums.RoomStatus;
import com.hotel.enums.RoomType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MySQLRoomRepository implements RoomRepository {

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public Room save(Room room) {
        String sql = """
            INSERT INTO rooms (id, room_number, type, price_per_night, status)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                room_number   = VALUES(room_number),
                type          = VALUES(type),
                price_per_night = VALUES(price_per_night),
                status        = VALUES(status)
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, room.getRoomId().toString());
            ps.setString(2, room.getRoomNumber());
            ps.setString(3, room.getType().name());
            ps.setDouble(4, room.getPricePerNight());
            ps.setString(5, room.getStatus().name());
            ps.executeUpdate();
            return room;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save room: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Room> findById(UUID id) {
        String sql = "SELECT * FROM rooms WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, id.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find room: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Room> findAll() {
        String sql = "SELECT * FROM rooms ORDER BY room_number";
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            List<Room> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list rooms: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Room> findByStatus(RoomStatus status) {
        String sql = "SELECT * FROM rooms WHERE status = ? ORDER BY room_number";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, status.name());
            ResultSet rs = ps.executeQuery();
            List<Room> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find rooms by status: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Room> findByRoomNumber(String roomNumber) {
        String sql = "SELECT * FROM rooms WHERE room_number = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, roomNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find room by number: " + e.getMessage(), e);
        }
    }

    public void delete(UUID id) {
        String sql = "DELETE FROM rooms WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, id.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete room: " + e.getMessage(), e);
        }
    }

    private Room mapRow(ResultSet rs) throws SQLException {
        return new Room(
            UUID.fromString(rs.getString("id")),
            rs.getString("room_number"),
            RoomType.valueOf(rs.getString("type")),
            rs.getDouble("price_per_night"),
            RoomStatus.valueOf(rs.getString("status"))
        );
    }
}
