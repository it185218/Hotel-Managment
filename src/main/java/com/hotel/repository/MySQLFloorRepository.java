package com.hotel.repository;

import com.hotel.db.DatabaseConnection;
import com.hotel.domain.Floor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MySQLFloorRepository implements FloorRepository {

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public Floor save(Floor floor) {
        String sql = """
            INSERT INTO floors (id, floor_number, description)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE
                floor_number = VALUES(floor_number),
                description  = VALUES(description)
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, floor.getFloorId().toString());
            ps.setInt(2,    floor.getFloorNumber());
            ps.setString(3, floor.getDescription());
            ps.executeUpdate();
            return floor;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save floor: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Floor> findById(UUID id) {
        String sql = "SELECT * FROM floors WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, id.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find floor: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Floor> findAll() {
        String sql = "SELECT * FROM floors ORDER BY floor_number";
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            List<Floor> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list floors: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(UUID id) {
        // First unassign all rooms on this floor
        String unassign = "UPDATE rooms SET floor_id = NULL WHERE floor_id = ?";
        String del      = "DELETE FROM floors WHERE id = ?";
        try {
            try (PreparedStatement ps = conn().prepareStatement(unassign)) {
                ps.setString(1, id.toString()); ps.executeUpdate();
            }
            try (PreparedStatement ps = conn().prepareStatement(del)) {
                ps.setString(1, id.toString()); ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete floor: " + e.getMessage(), e);
        }
    }

    private Floor mapRow(ResultSet rs) throws SQLException {
        return new Floor(
            UUID.fromString(rs.getString("id")),
            rs.getInt("floor_number"),
            rs.getString("description")
        );
    }
}
