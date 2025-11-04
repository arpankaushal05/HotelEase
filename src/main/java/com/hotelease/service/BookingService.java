package com.hotelease.service;

import com.hotelease.model.Booking;
import com.hotelease.model.Role;
import com.hotelease.model.User;
import com.hotelease.repository.BookingRepository;

import java.time.LocalDate;
import java.util.List;

public class BookingService {

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public List<Booking> getBookingsForUser(User user) {
        if (user == null || !isGuestUser(user)) {
            return getAllBookings();
        }
        String username = user.getUsername();
        if (username == null || username.isBlank()) {
            return List.of();
        }
        return bookingRepository.findByGuestUsername(username);
    }

    public Booking createOrUpdateBooking(Booking booking) {
        validateBooking(booking);
        return bookingRepository.save(booking);
    }

    public Booking createBooking(String guestName,
                                 String guestUsername,
                                 String roomNumber,
                                 LocalDate checkIn,
                                 LocalDate checkOut,
                                 String status) {
        Booking booking = new Booking(null, guestName, guestUsername, roomNumber, checkIn, checkOut, status);
        return createOrUpdateBooking(booking);
    }

    public Booking createGuestBooking(User user,
                                      String roomNumber,
                                      LocalDate checkIn,
                                      LocalDate checkOut) {
        if (user == null) {
            throw new IllegalArgumentException("Guest information is required");
        }
        String username = user.getUsername();
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Guest username is required");
        }
        String displayName = username;
        return createBooking(displayName, username, roomNumber, checkIn, checkOut, "PENDING");
    }

    private void validateBooking(Booking booking) {
        if (booking.getGuestName() == null || booking.getGuestName().isBlank()) {
            throw new IllegalArgumentException("Guest name is required");
        }
        if (booking.getGuestUsername() == null || booking.getGuestUsername().isBlank()) {
            throw new IllegalArgumentException("Guest username is required");
        }
        if (booking.getRoomNumber() == null || booking.getRoomNumber().isBlank()) {
            throw new IllegalArgumentException("Room number is required");
        }
        if (booking.getCheckIn() == null || booking.getCheckOut() == null) {
            throw new IllegalArgumentException("Check-in and Check-out dates are required");
        }
        if (booking.getCheckOut().isBefore(booking.getCheckIn())) {
            throw new IllegalArgumentException("Check-out date cannot be before check-in date");
        }
        if (booking.getStatus() == null || booking.getStatus().isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
    }

    private boolean isGuestUser(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(name -> "GUEST".equalsIgnoreCase(name));
    }
}
