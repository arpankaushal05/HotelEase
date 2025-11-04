package com.hotelease.repository;

import com.hotelease.model.HelpChatConversationSummary;
import com.hotelease.model.HelpChatMessage;

import java.util.List;

public interface HelpChatRepository {

    HelpChatMessage save(HelpChatMessage message);

    List<HelpChatMessage> findConversation(String guestUsername, String roomNumber, String recipientRole);

    void markConversationAsReadByStaff(String guestUsername, String roomNumber, String recipientRole);

    List<HelpChatConversationSummary> findConversationSummariesByRecipientRoles(List<String> recipientRoles);
}
