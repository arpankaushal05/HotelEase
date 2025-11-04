package com.hotelease.repository.jdbc;

import com.hotelease.config.DatabaseConfig;
import com.hotelease.model.HelpChatConversationSummary;
import com.hotelease.model.HelpChatMessage;
import com.hotelease.repository.HelpChatRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JdbcHelpChatRepository implements HelpChatRepository {

    private static final String INSERT_MESSAGE = "INSERT INTO help_chats (room_number, guest_username, recipient_role, recipient_username, sender_type, sender_username, message, created_at, is_read_by_staff) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_CONVERSATION = "SELECT id, room_number, guest_username, recipient_role, recipient_username, sender_type, sender_username, message, created_at, is_read_by_staff FROM help_chats WHERE guest_username = ? AND room_number = ? AND recipient_role = ? ORDER BY created_at";
    private static final String UPDATE_MARK_READ = "UPDATE help_chats SET is_read_by_staff = TRUE WHERE guest_username = ? AND room_number = ? AND recipient_role = ?";
    private static final String SELECT_SUMMARIES_BY_ROLES =
            "SELECT hc.room_number, hc.guest_username, hc.recipient_role, b.guest_name, " +
                    "MAX(hc.created_at) AS last_created, " +
                    "SUM(CASE WHEN hc.sender_type = 'GUEST' AND hc.is_read_by_staff = FALSE THEN 1 ELSE 0 END) AS unread_count " +
                    "FROM help_chats hc " +
                    "LEFT JOIN bookings b ON b.guest_username = hc.guest_username AND b.room_number = hc.room_number " +
                    "WHERE hc.recipient_role IN (%s) " +
                    "GROUP BY hc.room_number, hc.guest_username, hc.recipient_role, b.guest_name " +
                    "ORDER BY last_created DESC";

    @Override
    public HelpChatMessage save(HelpChatMessage message) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_MESSAGE, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, message.getRoomNumber());
            statement.setString(2, message.getGuestUsername());
            statement.setString(3, message.getRecipientRole());
            statement.setString(4, message.getRecipientUsername());
            statement.setString(5, message.getSenderType());
            statement.setString(6, message.getSenderUsername());
            statement.setString(7, message.getMessage());
            LocalDateTime createdAt = message.getCreatedAt() == null ? LocalDateTime.now() : message.getCreatedAt();
            statement.setTimestamp(8, Timestamp.valueOf(createdAt));
            statement.setBoolean(9, message.isReadByStaff());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    message.setId(keys.getLong(1));
                    message.setCreatedAt(createdAt);
                }
            }
            return message;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to insert help chat message", e);
        }
    }

    @Override
    public List<HelpChatMessage> findConversation(String guestUsername, String roomNumber, String recipientRole) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_CONVERSATION)) {
            statement.setString(1, guestUsername);
            statement.setString(2, roomNumber);
            statement.setString(3, recipientRole);
            try (ResultSet rs = statement.executeQuery()) {
                List<HelpChatMessage> messages = new ArrayList<>();
                while (rs.next()) {
                    messages.add(mapMessage(rs));
                }
                return messages;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load conversation", e);
        }
    }

    @Override
    public void markConversationAsReadByStaff(String guestUsername, String roomNumber, String recipientRole) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_MARK_READ)) {
            statement.setString(1, guestUsername);
            statement.setString(2, roomNumber);
            statement.setString(3, recipientRole);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to mark conversation as read", e);
        }
    }

    @Override
    public List<HelpChatConversationSummary> findConversationSummariesByRecipientRoles(List<String> recipientRoles) {
        if (recipientRoles == null || recipientRoles.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", recipientRoles.stream().map(role -> "?").toArray(String[]::new));
        String sql = String.format(SELECT_SUMMARIES_BY_ROLES, placeholders);
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < recipientRoles.size(); i++) {
                statement.setString(i + 1, recipientRoles.get(i));
            }
            try (ResultSet rs = statement.executeQuery()) {
                List<HelpChatConversationSummary> summaries = new ArrayList<>();
                while (rs.next()) {
                    String roomNumber = rs.getString("room_number");
                    String guestUsername = rs.getString("guest_username");
                    long unread = rs.getLong("unread_count");
                    LocalDateTime lastMessageAt = toLocalDateTime(rs.getTimestamp("last_created"));
                    String guestName = rs.getString("guest_name");
                    if (guestName == null || guestName.isBlank()) {
                        guestName = guestUsername;
                    }
                    String recipientRole = rs.getString("recipient_role");
                    summaries.add(new HelpChatConversationSummary(roomNumber, guestUsername, recipientRole, guestName, unread, lastMessageAt));
                }
                return summaries;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load help chat summaries", e);
        }
    }

    private HelpChatMessage mapMessage(ResultSet rs) throws SQLException {
        HelpChatMessage message = new HelpChatMessage();
        message.setId(rs.getLong("id"));
        message.setRoomNumber(rs.getString("room_number"));
        message.setGuestUsername(rs.getString("guest_username"));
        message.setRecipientRole(rs.getString("recipient_role"));
        message.setRecipientUsername(rs.getString("recipient_username"));
        message.setSenderType(rs.getString("sender_type"));
        message.setSenderUsername(rs.getString("sender_username"));
        message.setMessage(rs.getString("message"));
        message.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        message.setReadByStaff(rs.getBoolean("is_read_by_staff"));
        return message;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
