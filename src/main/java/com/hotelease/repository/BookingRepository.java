package com.hotelease.repository;

import com.hotelease.model.Booking;

import java.util.List;

public interface BookingRepository {
    List<Booking> findAll();

    List<Booking> findByGuestUsername(String guestUsername);

    Booking save(Booking booking);
}
