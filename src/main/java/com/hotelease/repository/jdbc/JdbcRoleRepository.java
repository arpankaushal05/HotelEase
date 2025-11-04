package com.hotelease.repository.jdbc;

import com.hotelease.config.DatabaseConfig;
import com.hotelease.model.Role;
import com.hotelease.repository.RoleRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcRoleRepository implements RoleRepository {

    private static final String SELECT_BY_NAME = "SELECT id, name FROM roles WHERE name = ?";
    private static final String SELECT_ALL = "SELECT id, name FROM roles ORDER BY name";
    private static final String INSERT_ROLE = "INSERT INTO roles (name) VALUES (?)";
    private static final String UPDATE_ROLE = "UPDATE roles SET name = ? WHERE id = ?";

    @Override
    public Optional<Role> findByName(String name) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_NAME)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(resultSet));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query role by name", e);
        }
    }

    @Override
    public List<Role> findAll() {
        List<Role> roles = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                roles.add(mapRow(resultSet));
            }
            return roles;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query roles", e);
        }
    }

    @Override
    public Role save(Role role) {
        try (Connection connection = DatabaseConfig.getConnection()) {
            if (role.getId() == null) {
                try (PreparedStatement statement = connection.prepareStatement(INSERT_ROLE, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, role.getName());
                    int affectedRows = statement.executeUpdate();
                    if (affectedRows == 0) {
                        throw new SQLException("Creating role failed, no rows affected");
                    }
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (keys.next()) {
                            role.setId(keys.getLong(1));
                        } else {
                            throw new SQLException("Creating role failed, no ID obtained");
                        }
                    }
                }
            } else {
                try (PreparedStatement statement = connection.prepareStatement(UPDATE_ROLE)) {
                    statement.setString(1, role.getName());
                    statement.setLong(2, role.getId());
                    statement.executeUpdate();
                }
            }
            return role;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save role", e);
        }
    }

    private Role mapRow(ResultSet resultSet) throws SQLException {
        Role role = new Role();
        role.setId(resultSet.getLong("id"));
        role.setName(resultSet.getString("name"));
        return role;
    }
}
