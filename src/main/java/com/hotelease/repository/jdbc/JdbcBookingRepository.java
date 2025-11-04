package com.hotelease.repository.jdbc;

import com.hotelease.config.DatabaseConfig;
import com.hotelease.model.Booking;
import com.hotelease.repository.BookingRepository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class JdbcBookingRepository implements BookingRepository {

    private static final String BASE_SELECT = "SELECT id, guest_name, guest_username, room_number, check_in, check_out, status FROM bookings";
    private static final String SELECT_ALL = BASE_SELECT + " ORDER BY check_in";
    private static final String SELECT_BY_GUEST = BASE_SELECT + " WHERE guest_username = ? ORDER BY check_in";
    private static final String INSERT_BOOKING = "INSERT INTO bookings (guest_name, guest_username, room_number, check_in, check_out, status) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_BOOKING = "UPDATE bookings SET guest_name = ?, guest_username = ?, room_number = ?, check_in = ?, check_out = ?, status = ? WHERE id = ?";

    @Override
    public List<Booking> findAll() {
        return queryBookings(SELECT_ALL, statement -> {
        });
    }

    @Override
    public List<Booking> findByGuestUsername(String guestUsername) {
        return queryBookings(SELECT_BY_GUEST, statement -> statement.setString(1, guestUsername));
    }

    @Override
    public Booking save(Booking booking) {
        try (Connection connection = DatabaseConfig.getConnection()) {
            if (booking.getId() == null) {
                return insertBooking(connection, booking);
            }
            return updateBooking(connection, booking);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save booking", e);
        }
    }

    private Booking insertBooking(Connection connection, Booking booking) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_BOOKING, Statement.RETURN_GENERATED_KEYS)) {
            setBookingParameters(statement, booking);
            int affected = statement.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Inserting booking failed, no rows affected");
            }
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    booking.setId(generatedKeys.getLong(1));
                }
            }
        }
        return booking;
    }

    private Booking updateBooking(Connection connection, Booking booking) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_BOOKING)) {
            setBookingParameters(statement, booking);
            statement.setLong(7, booking.getId());
            statement.executeUpdate();
        }
        return booking;
    }

    private void setBookingParameters(PreparedStatement statement, Booking booking) throws SQLException {
        statement.setString(1, booking.getGuestName());
        statement.setString(2, booking.getGuestUsername());
        statement.setString(3, booking.getRoomNumber());
        statement.setDate(4, Date.valueOf(booking.getCheckIn()));
        statement.setDate(5, Date.valueOf(booking.getCheckOut()));
        statement.setString(6, booking.getStatus());
    }

    private Booking mapRow(ResultSet resultSet) throws SQLException {
        Booking booking = new Booking();
        booking.setId(resultSet.getLong("id"));
        booking.setGuestName(resultSet.getString("guest_name"));
        booking.setGuestUsername(resultSet.getString("guest_username"));
        booking.setRoomNumber(resultSet.getString("room_number"));
        booking.setCheckIn(resultSet.getDate("check_in").toLocalDate());
        booking.setCheckOut(resultSet.getDate("check_out").toLocalDate());
        booking.setStatus(resultSet.getString("status"));
        return booking;
    }

    private List<Booking> queryBookings(String sql, StatementConfigurer configurer) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            configurer.configure(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Booking> bookings = new ArrayList<>();
                while (resultSet.next()) {
                    bookings.add(mapRow(resultSet));
                }
                return bookings;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load bookings", e);
        }
    }

    @FunctionalInterface
    private interface StatementConfigurer {
        void configure(PreparedStatement statement) throws SQLException;
    }
}
