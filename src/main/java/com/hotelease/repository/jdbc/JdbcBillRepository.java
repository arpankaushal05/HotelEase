package com.hotelease.repository.jdbc;

import com.hotelease.config.DatabaseConfig;
import com.hotelease.model.Bill;
import com.hotelease.repository.BillRepository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcBillRepository implements BillRepository {

    private static final String BASE_SELECT = "SELECT b.id, b.invoice_number, b.guest_name, b.guest_username, " +
            "u.phone AS guest_phone, b.amount, b.status, b.issued_date, b.due_date FROM bills b " +
            "LEFT JOIN users u ON u.username = b.guest_username";
    private static final String SELECT_ALL = BASE_SELECT + " ORDER BY b.issued_date DESC";
    private static final String SELECT_BY_STATUS = BASE_SELECT + " WHERE b.status = ? ORDER BY b.issued_date DESC";
    private static final String SELECT_BY_INVOICE = BASE_SELECT + " WHERE b.invoice_number = ?";
    private static final String SELECT_BY_GUEST = BASE_SELECT + " WHERE b.guest_username = ? ORDER BY b.issued_date DESC";
    private static final String SELECT_BY_GUEST_AND_STATUS = BASE_SELECT + " WHERE b.guest_username = ? AND b.status = ? ORDER BY b.issued_date DESC";
    private static final String INSERT_BILL = "INSERT INTO bills (invoice_number, guest_name, guest_username, amount, status, issued_date, due_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_BILL = "UPDATE bills SET invoice_number = ?, guest_name = ?, guest_username = ?, amount = ?, status = ?, issued_date = ?, due_date = ? WHERE id = ?";

    @Override
    public List<Bill> findAll() {
        return queryBills(SELECT_ALL, statement -> {
        });
    }

    @Override
    public List<Bill> findByStatus(String status) {
        return queryBills(SELECT_BY_STATUS, statement -> statement.setString(1, status));
    }

    @Override
    public Optional<Bill> findByInvoiceNumber(String invoiceNumber) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_INVOICE)) {
            statement.setString(1, invoiceNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to fetch bill", e);
        }
    }

    @Override
    public List<Bill> findByGuestUsername(String guestUsername) {
        return queryBills(SELECT_BY_GUEST, statement -> statement.setString(1, guestUsername));
    }

    @Override
    public List<Bill> findByGuestUsernameAndStatus(String guestUsername, String status) {
        return queryBills(SELECT_BY_GUEST_AND_STATUS, statement -> {
            statement.setString(1, guestUsername);
            statement.setString(2, status);
        });
    }

    @Override
    public Bill save(Bill bill) {
        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (bill.getId() == null) {
                    long id = insertBill(connection, bill);
                    bill.setId(id);
                } else {
                    updateBill(connection, bill);
                }
                connection.commit();
                return bill;
            } catch (SQLException e) {
                connection.rollback();
                throw new IllegalStateException("Failed to save bill", e);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save bill", e);
        }
    }

    private List<Bill> queryBills(String sql, StatementConfigurer configurer) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            configurer.configure(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Bill> bills = new ArrayList<>();
                while (resultSet.next()) {
                    bills.add(mapRow(resultSet));
                }
                return bills;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to fetch bills", e);
        }
    }

    private long insertBill(Connection connection, Bill bill) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_BILL, Statement.RETURN_GENERATED_KEYS)) {
            setParameters(statement, bill);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Insert bill failed, no ID obtained");
            }
        }
    }

    private void updateBill(Connection connection, Bill bill) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_BILL)) {
            setParameters(statement, bill);
            statement.setLong(8, bill.getId());
            statement.executeUpdate();
        }
    }

    private void setParameters(PreparedStatement statement, Bill bill) throws SQLException {
        statement.setString(1, bill.getInvoiceNumber());
        statement.setString(2, bill.getGuestName());
        statement.setString(3, bill.getGuestUsername());
        statement.setBigDecimal(4, bill.getAmount());
        statement.setString(5, bill.getStatus());
        statement.setDate(6, Date.valueOf(bill.getIssuedDate()));
        statement.setDate(7, Date.valueOf(bill.getDueDate()));
    }

    private Bill mapRow(ResultSet resultSet) throws SQLException {
        Bill bill = new Bill();
        bill.setId(resultSet.getLong("id"));
        bill.setInvoiceNumber(resultSet.getString("invoice_number"));
        bill.setGuestName(resultSet.getString("guest_name"));
        bill.setGuestUsername(resultSet.getString("guest_username"));
        bill.setGuestPhone(resultSet.getString("guest_phone"));
        bill.setAmount(resultSet.getBigDecimal("amount"));
        bill.setStatus(resultSet.getString("status"));
        bill.setIssuedDate(toLocalDate(resultSet.getDate("issued_date")));
        bill.setDueDate(toLocalDate(resultSet.getDate("due_date")));
        return bill;
    }

    private LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    @FunctionalInterface
    private interface StatementConfigurer {
        void configure(PreparedStatement statement) throws SQLException;
    }
}
