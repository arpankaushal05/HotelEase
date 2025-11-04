package com.hotelease.repository.jdbc;

import com.hotelease.config.DatabaseConfig;
import com.hotelease.model.Room;
import com.hotelease.repository.RoomRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcRoomRepository implements RoomRepository {

    private static final String SELECT_ALL = "SELECT id, room_number, room_type, status, rate FROM rooms ORDER BY room_number";
    private static final String SELECT_AVAILABLE = "SELECT id, room_number, room_type, status, rate FROM rooms WHERE status = 'AVAILABLE' ORDER BY room_number";
    private static final String SELECT_BY_NUMBER = "SELECT id, room_number, room_type, status, rate FROM rooms WHERE room_number = ?";
    private static final String INSERT_ROOM = "INSERT INTO rooms (room_number, room_type, status, rate) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_ROOM = "UPDATE rooms SET room_number = ?, room_type = ?, status = ?, rate = ? WHERE id = ?";
    private static final String DELETE_ROOM = "DELETE FROM rooms WHERE id = ?";

    @Override
    public List<Room> findAll() {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
             ResultSet resultSet = statement.executeQuery()) {
            List<Room> rooms = new ArrayList<>();
            while (resultSet.next()) {
                rooms.add(mapRow(resultSet));
            }
            return rooms;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to fetch rooms", e);
        }
    }

    @Override
    public List<Room> findAvailable() {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_AVAILABLE);
             ResultSet resultSet = statement.executeQuery()) {
            List<Room> rooms = new ArrayList<>();
            while (resultSet.next()) {
                rooms.add(mapRow(resultSet));
            }
            return rooms;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to fetch available rooms", e);
        }
    }

    @Override
    public Optional<Room> findByRoomNumber(String roomNumber) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_NUMBER)) {
            statement.setString(1, roomNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to fetch room by number", e);
        }
    }

    @Override
    public Room save(Room room) {
        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (room.getId() == null) {
                    long id = insertRoom(connection, room);
                    room.setId(id);
                } else {
                    updateRoom(connection, room);
                }
                connection.commit();
                return room;
            } catch (SQLException e) {
                connection.rollback();
                throw new IllegalStateException("Failed to save room", e);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save room", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_ROOM)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete room", e);
        }
    }

    private long insertRoom(Connection connection, Room room) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_ROOM, Statement.RETURN_GENERATED_KEYS)) {
            setParameters(statement, room);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Insert room failed, no ID obtained");
            }
        }
    }

    private void updateRoom(Connection connection, Room room) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_ROOM)) {
            setParameters(statement, room);
            statement.setLong(5, room.getId());
            statement.executeUpdate();
        }
    }

    private void setParameters(PreparedStatement statement, Room room) throws SQLException {
        statement.setString(1, room.getRoomNumber());
        statement.setString(2, room.getRoomType());
        statement.setString(3, room.getStatus());
        statement.setBigDecimal(4, room.getRate());
    }

    private Room mapRow(ResultSet resultSet) throws SQLException {
        Room room = new Room();
        room.setId(resultSet.getLong("id"));
        room.setRoomNumber(resultSet.getString("room_number"));
        room.setRoomType(resultSet.getString("room_type"));
        room.setStatus(resultSet.getString("status"));
        room.setRate(resultSet.getBigDecimal("rate"));
        return room;
    }
}
