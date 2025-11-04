package com.hotelease.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class HelpChatMessage {

    private Long id;
    private String roomNumber;
    private String guestUsername;
    private String recipientRole;
    private String recipientUsername;
    private String senderType;
    private String senderUsername;
    private String message;
    private LocalDateTime createdAt;
    private boolean readByStaff;

    public HelpChatMessage() {
    }

    public HelpChatMessage(Long id,
                           String roomNumber,
                           String guestUsername,
                           String recipientRole,
                           String recipientUsername,
                           String senderType,
                           String senderUsername,
                           String message,
                           LocalDateTime createdAt,
                           boolean readByStaff) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.guestUsername = guestUsername;
        this.recipientRole = recipientRole;
        this.recipientUsername = recipientUsername;
        this.senderType = senderType;
        this.senderUsername = senderUsername;
        this.message = message;
        this.createdAt = createdAt;
        this.readByStaff = readByStaff;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getGuestUsername() {
        return guestUsername;
    }

    public void setGuestUsername(String guestUsername) {
        this.guestUsername = guestUsername;
    }

    public String getRecipientRole() {
        return recipientRole;
    }

    public void setRecipientRole(String recipientRole) {
        this.recipientRole = recipientRole;
    }

    public String getRecipientUsername() {
        return recipientUsername;
    }

    public void setRecipientUsername(String recipientUsername) {
        this.recipientUsername = recipientUsername;
    }

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isReadByStaff() {
        return readByStaff;
    }

    public void setReadByStaff(boolean readByStaff) {
        this.readByStaff = readByStaff;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HelpChatMessage)) return false;
        HelpChatMessage that = (HelpChatMessage) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
