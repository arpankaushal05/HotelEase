package com.hotelease.repository;

import com.hotelease.model.Bill;

import java.util.List;
import java.util.Optional;

public interface BillRepository {

    List<Bill> findAll();

    List<Bill> findByStatus(String status);

    Optional<Bill> findByInvoiceNumber(String invoiceNumber);

    List<Bill> findByGuestUsername(String guestUsername);

    List<Bill> findByGuestUsernameAndStatus(String guestUsername, String status);

    Bill save(Bill bill);
}
