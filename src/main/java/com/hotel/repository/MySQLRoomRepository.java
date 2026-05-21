package com.hotel.repository;

import com.hotel.db.DatabaseConnection;
import com.hotel.domain.Floor;
import com.hotel.domain.Room;
import com.hotel.enums.RoomStatus;
import com.hotel.enums.RoomType;

import java.sql.*;
import java.util.*;

public class MySQLRoomRepository implements RoomRepository {

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public Room save(Room room) {
        String sql = """
            INSERT INTO rooms (id, room_number, type, price_per_night, status, floor_id)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                room_number     = VALUES(room_number),
                type            = VALUES(type),
                price_per_night = VALUES(price_per_night),
                status          = VALUES(status),
                floor_id        = VALUES(floor_id)
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, room.getRoomId().toString());
            ps.setString(2, room.getRoomNumber());
            ps.setString(3, room.getType().name());
            ps.setDouble(4, room.getPricePerNight());
            ps.setString(5, room.getStatus().name());
            if (room.getFloor() != null)
                ps.setString(6, room.getFloor().getFloorId().toString());
            else
                ps.setNull(6, Types.CHAR);
            ps.executeUpdate();
            return room;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save room: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Room> findById(UUID id) {
        String sql = buildJoin("r.id = ?");
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
        String sql = buildJoin(null) + " ORDER BY f.floor_number, r.room_number";
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
        String sql = buildJoin("r.status = ?") + " ORDER BY f.floor_number, r.room_number";
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
        String sql = buildJoin("r.room_number = ?");
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, roomNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find room by number: " + e.getMessage(), e);
        }
    }

    public List<Room> findByFloorId(UUID floorId) {
        String sql = buildJoin("r.floor_id = ?") + " ORDER BY r.room_number";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, floorId.toString());
            ResultSet rs = ps.executeQuery();
            List<Room> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find rooms by floor: " + e.getMessage(), e);
        }
    }

    public void delete(UUID id) {
        String sql = "DELETE FROM rooms WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, id.toString()); ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete room: " + e.getMessage(), e);
        }
    }

    private String buildJoin(String where) {
        String base = """
            SELECT r.id, r.room_number, r.type, r.price_per_night, r.status,
                   f.id AS floor_id, f.floor_number, f.description AS floor_desc
            FROM rooms r
            LEFT JOIN floors f ON r.floor_id = f.id
            """;
        return where == null ? base : base + " WHERE " + where;
    }

    private Room mapRow(ResultSet rs) throws SQLException {
        Floor floor = null;
        String floorId = rs.getString("floor_id");
        if (floorId != null && !rs.wasNull()) {
            floor = new Floor(
                UUID.fromString(floorId),
                rs.getInt("floor_number"),
                rs.getString("floor_desc")
            );
        }
        return new Room(
            UUID.fromString(rs.getString("id")),
            rs.getString("room_number"),
            RoomType.valueOf(rs.getString("type")),
            rs.getDouble("price_per_night"),
            RoomStatus.valueOf(rs.getString("status")),
            floor
        );
    }
}
