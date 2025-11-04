package com.hotelease.repository;

import com.hotelease.model.Role;

import java.util.List;
import java.util.Optional;

public interface RoleRepository {
    Optional<Role> findByName(String name);

    List<Role> findAll();

    Role save(Role role);
}
