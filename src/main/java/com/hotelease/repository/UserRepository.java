package com.hotelease.repository;

import com.hotelease.model.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    User save(User user);
}
