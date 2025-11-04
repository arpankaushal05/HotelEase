package com.hotelease.repository.jdbc;

import com.hotelease.config.DatabaseConfig;
import com.hotelease.model.Role;
import com.hotelease.model.User;
import com.hotelease.repository.UserRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class JdbcUserRepository implements UserRepository {

    private static final String SELECT_BY_USERNAME = "SELECT u.id, u.username, u.password_hash, u.email, u.phone, u.active " +
            "FROM users u WHERE u.username = ?";

    private static final String SELECT_BY_EMAIL = "SELECT u.id, u.username, u.password_hash, u.email, u.phone, u.active " +
            "FROM users u WHERE u.email = ?";

    private static final String SELECT_ROLES = "SELECT r.id, r.name FROM roles r " +
            "INNER JOIN user_roles ur ON ur.role_id = r.id WHERE ur.user_id = ?";

    private static final String INSERT_USER = "INSERT INTO users (username, password_hash, email, phone, active) VALUES (?, ?, ?, ?, ?)";

    private static final String INSERT_USER_ROLE = "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)";

    @Override
    public Optional<User> findByUsername(String username) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_USERNAME)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                User user = mapUser(resultSet);
                user.getRoles().addAll(findRoles(connection, user.getId()));
                return Optional.of(user);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query user by username", e);
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_EMAIL)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                User user = mapUser(resultSet);
                user.getRoles().addAll(findRoles(connection, user.getId()));
                return Optional.of(user);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query user by email", e);
        }
    }

    @Override
    public User save(User user) {
        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (user.getId() == null) {
                    long id = insertUser(connection, user);
                    user.setId(id);
                } else {
                    updateUser(connection, user);
                }
                syncRoles(connection, user);
                connection.commit();
                return user;
            } catch (SQLException e) {
                connection.rollback();
                throw new IllegalStateException("Failed to save user", e);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save user", e);
        }
    }

    private long insertUser(Connection connection, User user) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_USER, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPasswordHash());
            statement.setString(3, user.getEmail());
            statement.setString(4, user.getPhone());
            statement.setBoolean(5, user.isActive());
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected");
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Creating user failed, no ID obtained");
            }
        }
    }

    private void updateUser(Connection connection, User user) throws SQLException {
        String updateSql = "UPDATE users SET password_hash = ?, email = ?, phone = ?, active = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
            statement.setString(1, user.getPasswordHash());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPhone());
            statement.setBoolean(4, user.isActive());
            statement.setLong(5, user.getId());
            statement.executeUpdate();
        }
    }

    private void syncRoles(Connection connection, User user) throws SQLException {
        try (PreparedStatement deleteStmt = connection.prepareStatement("DELETE FROM user_roles WHERE user_id = ?")) {
            deleteStmt.setLong(1, user.getId());
            deleteStmt.executeUpdate();
        }

        if (user.getRoles().isEmpty()) {
            return;
        }

        try (PreparedStatement insertStmt = connection.prepareStatement(INSERT_USER_ROLE)) {
            for (Role role : user.getRoles()) {
                insertStmt.setLong(1, user.getId());
                insertStmt.setLong(2, role.getId());
                insertStmt.addBatch();
            }
            insertStmt.executeBatch();
        }
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setId(resultSet.getLong("id"));
        user.setUsername(resultSet.getString("username"));
        user.setPasswordHash(resultSet.getString("password_hash"));
        user.setEmail(resultSet.getString("email"));
        user.setPhone(resultSet.getString("phone"));
        user.setActive(resultSet.getBoolean("active"));
        return user;
    }

    private Set<Role> findRoles(Connection connection, Long userId) throws SQLException {
        Set<Role> roles = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(SELECT_ROLES)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Role role = new Role();
                    role.setId(resultSet.getLong("id"));
                    role.setName(resultSet.getString("name"));
                    roles.add(role);
                }
            }
        }
        return roles;
    }
}
