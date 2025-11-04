package com.hotelease.service;

import com.hotelease.model.Room;
import com.hotelease.repository.RoomRepository;

import java.math.BigDecimal;
import java.util.List;

public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public List<Room> getAvailableRooms() {
        return roomRepository.findAvailable();
    }

    public Room saveRoom(Room room) {
        validateRoom(room);
        return roomRepository.save(room);
    }

    public Room createRoom(String roomNumber, String roomType, String status, BigDecimal rate) {
        Room room = new Room(null, roomNumber, roomType, status, rate);
        return saveRoom(room);
    }

    public Room markRoomStatus(Room room, String status) {
        if (room == null) {
            throw new IllegalArgumentException("Room must be provided");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        room.setStatus(status.toUpperCase());
        return saveRoom(room);
    }

    public void deleteRoom(Long id) {
        roomRepository.deleteById(id);
    }

    private void validateRoom(Room room) {
        if (room.getRoomNumber() == null || room.getRoomNumber().isBlank()) {
            throw new IllegalArgumentException("Room number is required");
        }
        if (room.getRoomType() == null || room.getRoomType().isBlank()) {
            throw new IllegalArgumentException("Room type is required");
        }
        if (room.getStatus() == null || room.getStatus().isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        if (room.getRate() == null || room.getRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Rate must be zero or positive");
        }
    }
}
