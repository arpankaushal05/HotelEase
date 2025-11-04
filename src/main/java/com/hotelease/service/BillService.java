package com.hotelease.service;

import com.hotelease.model.Bill;
import com.hotelease.model.Role;
import com.hotelease.model.User;
import com.hotelease.repository.BillRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class BillService {

    private final BillRepository billRepository;

    public BillService(BillRepository billRepository) {
        this.billRepository = billRepository;
    }

    public List<Bill> getAllBills() {
        return billRepository.findAll();
    }

    public List<Bill> getBillsByStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return getAllBills();
        }
        return billRepository.findByStatus(status.toUpperCase());
    }

    public List<Bill> getBillsForUser(User user, String status) {
        if (user == null || !isGuestUser(user)) {
            return getBillsByStatus(status);
        }

        String username = user.getUsername();
        if (username == null || username.isBlank()) {
            return List.of();
        }

        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return billRepository.findByGuestUsername(username);
        }
        return billRepository.findByGuestUsernameAndStatus(username, status.toUpperCase());
    }

    public Bill save(Bill bill) {
        validateBill(bill);
        return billRepository.save(bill);
    }

    public Bill markAsPaid(Bill bill) {
        if (bill == null) {
            throw new IllegalArgumentException("Bill must be provided");
        }
        bill.setStatus("PAID");
        if (bill.getDueDate() == null || bill.getDueDate().isBefore(LocalDate.now())) {
            bill.setDueDate(LocalDate.now());
        }
        return billRepository.save(bill);
    }

    private void validateBill(Bill bill) {
        if (bill.getInvoiceNumber() == null || bill.getInvoiceNumber().isBlank()) {
            throw new IllegalArgumentException("Invoice number is required");
        }
        if (bill.getGuestName() == null || bill.getGuestName().isBlank()) {
            throw new IllegalArgumentException("Guest name is required");
        }
        if (bill.getGuestUsername() == null || bill.getGuestUsername().isBlank()) {
            throw new IllegalArgumentException("Guest username is required");
        }
        if (bill.getAmount() == null || bill.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be zero or positive");
        }
        if (bill.getStatus() == null || bill.getStatus().isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        if (bill.getIssuedDate() == null) {
            throw new IllegalArgumentException("Issued date is required");
        }
        if (bill.getDueDate() == null) {
            throw new IllegalArgumentException("Due date is required");
        }
    }

    private boolean isGuestUser(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(name -> "GUEST".equalsIgnoreCase(name));
    }
}
