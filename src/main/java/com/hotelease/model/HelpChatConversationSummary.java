package com.hotelease.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class HelpChatConversationSummary {

    private final String roomNumber;
    private final String guestUsername;
    private final String recipientRole;
    private final String guestName;
    private final long unreadCount;
    private final LocalDateTime lastMessageAt;

    public HelpChatConversationSummary(String roomNumber,
                                       String guestUsername,
                                       String recipientRole,
                                       String guestName,
                                       long unreadCount,
                                       LocalDateTime lastMessageAt) {
        this.roomNumber = roomNumber;
        this.guestUsername = guestUsername;
        this.recipientRole = recipientRole;
        this.guestName = guestName;
        this.unreadCount = unreadCount;
        this.lastMessageAt = lastMessageAt;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getGuestUsername() {
        return guestUsername;
    }

    public String getRecipientRole() {
        return recipientRole;
    }

    public String getGuestName() {
        return guestName;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HelpChatConversationSummary)) return false;
        HelpChatConversationSummary that = (HelpChatConversationSummary) o;
        return Objects.equals(roomNumber, that.roomNumber)
                && Objects.equals(guestUsername, that.guestUsername)
                && Objects.equals(recipientRole, that.recipientRole);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomNumber, guestUsername, recipientRole);
    }
}
