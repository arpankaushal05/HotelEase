package com.hotelease.repository;

import com.hotelease.model.Room;

import java.util.List;
import java.util.Optional;

public interface RoomRepository {

    List<Room> findAll();

    List<Room> findAvailable();

    Optional<Room> findByRoomNumber(String roomNumber);

    Room save(Room room);

    void deleteById(Long id);
}
