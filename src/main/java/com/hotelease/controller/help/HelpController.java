package com.hotelease.controller.help;

import com.hotelease.controller.dashboard.DashboardController;
import com.hotelease.model.Booking;
import com.hotelease.model.HelpChatConversationSummary;
import com.hotelease.model.HelpChatMessage;
import com.hotelease.model.Role;
import com.hotelease.model.User;
import com.hotelease.service.AuthService;
import com.hotelease.service.BookingService;
import com.hotelease.service.HelpChatService;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class HelpController {

    private static final DateTimeFormatter MESSAGE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @FXML
    private VBox guestSelectionContainer;

    @FXML
    private VBox staffConversationContainer;

    @FXML
    private ListView<Booking> guestBookingListView;

    @FXML
    private ChoiceBox<RecipientOption> recipientChoiceBox;

    @FXML
    private TableView<HelpChatConversationSummary> conversationTable;

    @FXML
    private TableColumn<HelpChatConversationSummary, String> roomColumn;

    @FXML
    private TableColumn<HelpChatConversationSummary, String> guestColumn;

    @FXML
    private TableColumn<HelpChatConversationSummary, String> roleColumn;

    @FXML
    private TableColumn<HelpChatConversationSummary, Number> unreadColumn;

    @FXML
    private ListView<String> messageListView;

    @FXML
    private TextArea messageInput;

    @FXML
    private Button sendButton;

    @FXML
    private Button refreshButton;

    @FXML
    private Button backButton;

    @FXML
    private Label statusLabel;

    private final ObservableList<Booking> guestBookings = FXCollections.observableArrayList();
    private final ObservableList<HelpChatConversationSummary> staffConversations = FXCollections.observableArrayList();
    private final ObservableList<String> messages = FXCollections.observableArrayList();

    private final BookingService bookingService;
    private final HelpChatService helpChatService;

    private Stage stage;
    private String stylesheet;
    private User user;
    private AuthService authService;

    private Booking selectedBooking;
    private HelpChatConversationSummary selectedConversation;
    private boolean suppressConversationSelection;

    public HelpController(BookingService bookingService, HelpChatService helpChatService) {
        this.bookingService = bookingService;
        this.helpChatService = helpChatService;
    }

    @FXML
    private void initialize() {
        messageListView.setItems(messages);
        messageListView.setPlaceholder(new Label("Select a conversation to view messages."));

        guestBookingListView.setItems(guestBookings);
        guestBookingListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Booking item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("Room %s (%s - %s)",
                            item.getRoomNumber(),
                            Optional.ofNullable(item.getCheckIn()).orElse(null),
                            Optional.ofNullable(item.getCheckOut()).orElse(null)));
                }
            }
        });

        conversationTable.setItems(staffConversations);
        roomColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRoomNumber()));
        guestColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getGuestName()));
        roleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRecipientRole()));
        unreadColumn.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getUnreadCount()));

        guestBookingListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            selectedBooking = newSel;
            loadGuestConversation();
            updateSendButtonState();
        });

        if (recipientChoiceBox != null) {
            recipientChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
                loadGuestConversation();
                updateSendButtonState();
            });
        }

        conversationTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (suppressConversationSelection) {
                return;
            }
            selectedConversation = newSel;
            if (newSel != null) {
                loadConversation(newSel.getGuestUsername(), newSel.getRoomNumber(), newSel.getRecipientRole(), true);
            } else {
                messages.clear();
            }
            updateSendButtonState();
        });

        messageInput.textProperty().addListener((obs, oldText, newText) -> updateSendButtonState());
        updateSendButtonState();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setStylesheet(String stylesheet) {
        this.stylesheet = stylesheet;
    }

    public void setUser(User user) {
        this.user = user;
        boolean guest = isGuestUser();
        configureVisibility(guest);
        if (guest) {
            loadGuestData();
        } else {
            refreshStaffConversations();
        }
    }

    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    @FXML
    private void handleSend(ActionEvent event) {
        clearStatus();
        String messageText = messageInput.getText();
        try {
            if (messageText == null || messageText.isBlank()) {
                showError("Message cannot be blank.");
                return;
            }
            if (isGuestUser()) {
                if (selectedBooking == null) {
                    showError("Select a booking to start a chat.");
                    return;
                }
                RecipientOption option = recipientChoiceBox.getSelectionModel().getSelectedItem();
                if (option == null) {
                    showError("Select who you want to chat with.");
                    return;
                }
                helpChatService.sendGuestMessage(user.getUsername(), selectedBooking.getRoomNumber(), option.roleCode(), messageText);
                loadGuestConversation();
            } else {
                if (selectedConversation == null) {
                    showError("Select a conversation to reply to.");
                    return;
                }
                helpChatService.sendStaffMessage(selectedConversation.getGuestUsername(),
                        selectedConversation.getRoomNumber(),
                        selectedConversation.getRecipientRole(),
                        user.getUsername(),
                        messageText);
                loadConversation(selectedConversation.getGuestUsername(),
                        selectedConversation.getRoomNumber(),
                        selectedConversation.getRecipientRole(),
                        false);
                refreshStaffConversations();
            }
            messageInput.clear();
            updateSendButtonState();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Failed to send message.");
        }
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        clearStatus();
        if (isGuestUser()) {
            loadGuestData();
        } else {
            refreshStaffConversations();
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        if (stage == null) {
            showError("Stage unavailable.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/dashboard.fxml"));
            Pane root = loader.load();
            DashboardController controller = loader.getController();
            controller.setStage(stage);
            controller.setStylesheet(stylesheet);
            controller.setAuthService(authService);
            controller.setUser(user);
            if (stage.getScene() == null) {
                Scene scene = new Scene(root, 900, 650);
                if (stylesheet != null) {
                    scene.getStylesheets().add(stylesheet);
                }
                stage.setScene(scene);
            } else {
                if (stylesheet != null && !stage.getScene().getStylesheets().contains(stylesheet)) {
                    stage.getScene().getStylesheets().add(stylesheet);
                }
                stage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            showError("Failed to return to dashboard.");
        }
    }

    private void loadGuestData() {
        guestBookings.setAll(bookingService.getBookingsForUser(user));
        if (recipientChoiceBox != null && recipientChoiceBox.getItems().isEmpty()) {
            recipientChoiceBox.setItems(FXCollections.observableArrayList(
                    new RecipientOption("Receptionist", "RECEPTIONIST"),
                    new RecipientOption("Manager", "MANAGER")
            ));
        }
        if (!guestBookings.contains(selectedBooking)) {
            selectedBooking = null;
            guestBookingListView.getSelectionModel().clearSelection();
        }
        if (selectedBooking == null && !guestBookings.isEmpty()) {
            guestBookingListView.getSelectionModel().selectFirst();
        } else {
            loadGuestConversation();
        }
    }

    private void refreshStaffConversations() {
        List<String> roles = getStaffRecipientRoles();
        List<HelpChatConversationSummary> updated = helpChatService.getStaffConversationSummaries(roles);
        suppressConversationSelection = true;
        staffConversations.setAll(updated);

        HelpChatConversationSummary matching = selectedConversation == null ? null : staffConversations.stream()
                .filter(summary -> summary.equals(selectedConversation))
                .findFirst()
                .orElse(null);

        if (matching != null) {
            selectedConversation = matching;
            conversationTable.getSelectionModel().select(matching);
        } else if (!staffConversations.isEmpty()) {
            selectedConversation = staffConversations.get(0);
            conversationTable.getSelectionModel().select(selectedConversation);
        } else {
            selectedConversation = null;
            conversationTable.getSelectionModel().clearSelection();
            messages.clear();
        }
        suppressConversationSelection = false;

        if (selectedConversation != null) {
            loadConversation(selectedConversation.getGuestUsername(),
                    selectedConversation.getRoomNumber(),
                    selectedConversation.getRecipientRole(),
                    false);
        }
    }

    private void loadGuestConversation() {
        if (!isGuestUser()) {
            return;
        }
        Booking booking = selectedBooking;
        RecipientOption option = recipientChoiceBox != null ? recipientChoiceBox.getSelectionModel().getSelectedItem() : null;
        if (booking == null || option == null) {
            messages.clear();
            return;
        }
        loadConversation(user.getUsername(), booking.getRoomNumber(), option.roleCode(), false);
    }

    private void loadConversation(String guestUsername,
                                  String roomNumber,
                                  String recipientRole,
                                  boolean markReadForStaff) {
        try {
            List<HelpChatMessage> chatMessages = helpChatService.getConversation(guestUsername, roomNumber, recipientRole);
            messages.setAll(chatMessages.stream()
                    .map(msg -> String.format("[%s | %s] %s",
                            formatSender(msg),
                            msg.getCreatedAt() == null ? "" : MESSAGE_TIME_FORMAT.format(msg.getCreatedAt()),
                            msg.getMessage()))
                    .collect(Collectors.toList()));
            if (!chatMessages.isEmpty() && !isGuestUser() && markReadForStaff) {
                helpChatService.markConversationAsRead(guestUsername, roomNumber, recipientRole);
                refreshStaffConversations();
            }
        } catch (Exception ex) {
            showError("Failed to load messages.");
        }
    }

    private void configureVisibility(boolean guest) {
        if (guestSelectionContainer != null) {
            guestSelectionContainer.setVisible(guest);
            guestSelectionContainer.setManaged(guest);
        }
        if (staffConversationContainer != null) {
            staffConversationContainer.setVisible(!guest);
            staffConversationContainer.setManaged(!guest);
        }
    }

    private void updateSendButtonState() {
        boolean hasMessage = messageInput != null && messageInput.getText() != null && !messageInput.getText().isBlank();
        boolean enabled;
        if (isGuestUser()) {
            enabled = hasMessage && selectedBooking != null && recipientChoiceBox != null
                    && recipientChoiceBox.getSelectionModel().getSelectedItem() != null;
        } else {
            enabled = hasMessage && selectedConversation != null;
        }
        if (sendButton != null) {
            sendButton.setDisable(!enabled);
        }
    }

    private List<String> getStaffRecipientRoles() {
        if (user == null) {
            return List.of();
        }
        boolean isAdmin = userHasRole("ADMIN");
        boolean isManager = userHasRole("MANAGER");
        boolean isReceptionist = userHasRole("RECEPTIONIST");
        if (isAdmin) {
            return List.of("RECEPTIONIST", "MANAGER", "ADMIN");
        }
        if (isManager && isReceptionist) {
            return List.of("MANAGER", "RECEPTIONIST");
        }
        if (isManager) {
            return List.of("MANAGER");
        }
        if (isReceptionist) {
            return List.of("RECEPTIONIST");
        }
        return List.of();
    }

    private boolean isGuestUser() {
        return userHasRole("GUEST") && !userHasRole("RECEPTIONIST") && !userHasRole("MANAGER") && !userHasRole("ADMIN");
    }

    private boolean userHasRole(String roleName) {
        if (user == null) {
            return false;
        }
        return user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(name -> roleName.equalsIgnoreCase(name));
    }

    private String formatSender(HelpChatMessage message) {
        if ("GUEST".equalsIgnoreCase(message.getSenderType())) {
            return "Guest " + message.getSenderUsername();
        }
        return message.getSenderUsername();
    }

    private void clearStatus() {
        if (statusLabel != null) {
            statusLabel.setText("");
            statusLabel.getStyleClass().removeAll("error-label", "success-label");
        }
    }

    private void showError(String message) {
        if (statusLabel == null) {
            return;
        }
        statusLabel.getStyleClass().removeAll("success-label");
        statusLabel.getStyleClass().add("error-label");
        statusLabel.setText(message);
    }

    private record RecipientOption(String displayText, String roleCode) {
        @Override
        public String toString() {
            return displayText;
        }
    }
}
