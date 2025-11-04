package com.hotelease.service;

import com.hotelease.model.HelpChatConversationSummary;
import com.hotelease.model.HelpChatMessage;
import com.hotelease.repository.HelpChatRepository;
import com.hotelease.repository.RoomRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class HelpChatService {

    private final HelpChatRepository helpChatRepository;
    private final RoomRepository roomRepository;

    public HelpChatService(HelpChatRepository helpChatRepository, RoomRepository roomRepository) {
        this.helpChatRepository = Objects.requireNonNull(helpChatRepository);
        this.roomRepository = Objects.requireNonNull(roomRepository);
    }

    public HelpChatMessage sendGuestMessage(String guestUsername,
                                            String roomNumber,
                                            String recipientRole,
                                            String message) {
        validateGuestContext(guestUsername, roomNumber, message);
        HelpChatMessage chatMessage = buildMessage(roomNumber,
                guestUsername,
                recipientRole,
                null,
                "GUEST",
                guestUsername,
                message,
                false);
        return helpChatRepository.save(chatMessage);
    }

    public HelpChatMessage sendStaffMessage(String guestUsername,
                                            String roomNumber,
                                            String recipientRole,
                                            String staffUsername,
                                            String message) {
        if (staffUsername == null || staffUsername.isBlank()) {
            throw new IllegalArgumentException("Staff username is required");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be blank");
        }
        HelpChatMessage chatMessage = buildMessage(roomNumber,
                guestUsername,
                recipientRole,
                staffUsername,
                "STAFF",
                staffUsername,
                message,
                true);
        return helpChatRepository.save(chatMessage);
    }

    public List<HelpChatMessage> getConversation(String guestUsername,
                                                 String roomNumber,
                                                 String recipientRole) {
        return helpChatRepository.findConversation(guestUsername, roomNumber, recipientRole);
    }

    public void markConversationAsRead(String guestUsername,
                                       String roomNumber,
                                       String recipientRole) {
        helpChatRepository.markConversationAsReadByStaff(guestUsername, roomNumber, recipientRole);
    }

    public List<HelpChatConversationSummary> getStaffConversationSummaries(List<String> recipientRoles) {
        return helpChatRepository.findConversationSummariesByRecipientRoles(recipientRoles);
    }

    private HelpChatMessage buildMessage(String roomNumber,
                                         String guestUsername,
                                         String recipientRole,
                                         String recipientUsername,
                                         String senderType,
                                         String senderUsername,
                                         String message,
                                         boolean markReadForStaff) {
        HelpChatMessage chatMessage = new HelpChatMessage();
        chatMessage.setRoomNumber(roomNumber);
        chatMessage.setGuestUsername(guestUsername);
        chatMessage.setRecipientRole(recipientRole);
        chatMessage.setRecipientUsername(recipientUsername);
        chatMessage.setSenderType(senderType);
        chatMessage.setSenderUsername(senderUsername);
        chatMessage.setMessage(message);
        chatMessage.setCreatedAt(LocalDateTime.now());
        chatMessage.setReadByStaff(markReadForStaff);
        return chatMessage;
    }

    private void validateGuestContext(String guestUsername, String roomNumber, String message) {
        if (guestUsername == null || guestUsername.isBlank()) {
            throw new IllegalArgumentException("Guest username is required");
        }
        if (roomNumber == null || roomNumber.isBlank()) {
            throw new IllegalArgumentException("Room number is required");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be blank");
        }
        roomRepository.findByRoomNumber(roomNumber)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
    }
}
