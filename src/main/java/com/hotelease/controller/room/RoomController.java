package com.hotelease.controller.room;

import com.hotelease.controller.dashboard.DashboardController;
import com.hotelease.model.Role;
import com.hotelease.model.Room;
import com.hotelease.model.User;
import com.hotelease.service.AuthService;
import com.hotelease.service.BookingService;
import com.hotelease.service.RoomService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

public class RoomController {

    @FXML
    private TableView<Room> roomTable;

    @FXML
    private TableColumn<Room, String> numberColumn;

    @FXML
    private TableColumn<Room, String> typeColumn;

    @FXML
    private TableColumn<Room, String> statusColumn;

    @FXML
    private TableColumn<Room, BigDecimal> rateColumn;

    @FXML
    private TextField roomNumberField;

    @FXML
    private ComboBox<String> roomTypeCombo;

    @FXML
    private ComboBox<String> statusCombo;

    @FXML
    private TextField rateField;

    @FXML
    private Button saveButton;

    @FXML
    private Button clearButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Label statusLabel;

    @FXML
    private Button backButton;

    @FXML
    private VBox adminControlsContainer;

    @FXML
    private VBox guestDetailsContainer;

    @FXML
    private Label detailRoomNumberLabel;

    @FXML
    private Label detailRoomTypeLabel;

    @FXML
    private Label detailRoomStatusLabel;

    @FXML
    private Label detailRoomRateLabel;

    @FXML
    private DatePicker guestCheckInPicker;

    @FXML
    private DatePicker guestCheckOutPicker;

    @FXML
    private Button bookButton;

    private final ObservableList<Room> rooms = FXCollections.observableArrayList();
    private final RoomService roomService;
    private final BookingService bookingService;
    private Stage stage;
    private String stylesheet;
    private User user;
    private AuthService authService;

    public RoomController(RoomService roomService, BookingService bookingService) {
        this.roomService = roomService;
        this.bookingService = bookingService;
    }

    @FXML
    private void initialize() {
        numberColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getRoomNumber()));
        typeColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getRoomType()));
        statusColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getStatus()));
        rateColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getRate()));

        roomTypeCombo.setItems(FXCollections.observableArrayList("SINGLE", "DOUBLE", "SUITE"));
        statusCombo.setItems(FXCollections.observableArrayList("AVAILABLE", "OCCUPIED", "MAINTENANCE"));

        roomTable.setItems(rooms);
        roomTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (!isGuestUser()) {
                populateForm(newSel);
            }
            updateGuestDetails(newSel);
        });
        refreshRooms();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setStylesheet(String stylesheet) {
        this.stylesheet = stylesheet;
    }

    public void setUser(User user) {
        this.user = user;
        configureForUser();
    }

    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    @FXML
    private void handleSave(ActionEvent event) {
        try {
            String roomNumber = roomNumberField.getText();
            String roomType = roomTypeCombo.getValue();
            String status = statusCombo.getValue();
            BigDecimal rate = parseRate();

            Room selected = roomTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                roomService.createRoom(roomNumber, roomType, status, rate);
            } else {
                selected.setRoomNumber(roomNumber);
                selected.setRoomType(roomType);
                selected.setStatus(status);
                selected.setRate(rate);
                roomService.saveRoom(selected);
            }
            showMessage("Room saved successfully.", false);
            clearForm();
            refreshRooms();
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage(), true);
        } catch (Exception ex) {
            showMessage("Failed to save room.", true);
        }
    }

    @FXML
    private void handleClear(ActionEvent event) {
        clearForm();
        roomTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        Room selected = roomTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Select a room to delete.", true);
            return;
        }
        try {
            roomService.deleteRoom(selected.getId());
            showMessage("Room deleted.", false);
            clearForm();
            refreshRooms();
        } catch (Exception ex) {
            showMessage("Failed to delete room.", true);
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        if (stage == null) {
            showMessage("Stage unavailable.", true);
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
                Scene scene = new Scene(root, 800, 600);
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
            showMessage("Failed to return to dashboard.", true);
        }
    }

    private BigDecimal parseRate() {
        try {
            return new BigDecimal(rateField.getText());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Rate must be a valid number");
        }
    }

    private void refreshRooms() {
        if (roomService == null) {
            return;
        }
        if (isGuestUser()) {
            rooms.setAll(roomService.getAvailableRooms());
            clearGuestDetails();
        } else {
            rooms.setAll(roomService.getAllRooms());
        }
    }

    private void clearForm() {
        if (isGuestUser()) {
            return;
        }
        roomNumberField.clear();
        roomTypeCombo.getSelectionModel().clearSelection();
        statusCombo.getSelectionModel().clearSelection();
        rateField.clear();
        showMessage("", false);
    }

    private void populateForm(Room room) {
        if (room == null || isGuestUser()) {
            clearForm();
            return;
        }
        roomNumberField.setText(room.getRoomNumber());
        roomTypeCombo.setValue(room.getRoomType());
        statusCombo.setValue(room.getStatus());
        rateField.setText(room.getRate() != null ? room.getRate().toPlainString() : "");
    }

    private void showMessage(String message, boolean error) {
        statusLabel.getStyleClass().removeAll("error-label", "success-label");
        statusLabel.setText(message == null ? "" : message);
        if (message == null || message.isBlank()) {
            return;
        }
        statusLabel.getStyleClass().add(error ? "error-label" : "success-label");
    }

    private void configureForUser() {
        boolean guest = isGuestUser();
        if (adminControlsContainer != null) {
            adminControlsContainer.setManaged(!guest);
            adminControlsContainer.setVisible(!guest);
        }
        if (guestDetailsContainer != null) {
            guestDetailsContainer.setManaged(guest);
            guestDetailsContainer.setVisible(guest);
        }
        if (guest) {
            bookButton.setDisable(true);
            if (guestCheckInPicker != null) {
                guestCheckInPicker.setValue(null);
            }
            if (guestCheckOutPicker != null) {
                guestCheckOutPicker.setValue(null);
            }
        }
        roomTable.getSelectionModel().clearSelection();
        refreshRooms();
    }

    private void updateGuestDetails(Room room) {
        if (!isGuestUser() || guestDetailsContainer == null) {
            return;
        }
        if (room == null) {
            clearGuestDetails();
            return;
        }
        detailRoomNumberLabel.setText(room.getRoomNumber());
        detailRoomTypeLabel.setText(room.getRoomType());
        detailRoomStatusLabel.setText(room.getStatus());
        detailRoomRateLabel.setText(room.getRate() != null ? room.getRate().toPlainString() : "-");
        bookButton.setDisable(false);
    }

    private void clearGuestDetails() {
        if (detailRoomNumberLabel != null) {
            detailRoomNumberLabel.setText("-");
        }
        if (detailRoomTypeLabel != null) {
            detailRoomTypeLabel.setText("-");
        }
        if (detailRoomStatusLabel != null) {
            detailRoomStatusLabel.setText("-");
        }
        if (detailRoomRateLabel != null) {
            detailRoomRateLabel.setText("-");
        }
        if (bookButton != null) {
            bookButton.setDisable(true);
        }
    }

    @FXML
    private void handleBook(ActionEvent event) {
        if (!isGuestUser()) {
            return;
        }
        Room selected = roomTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Select a room to book.", true);
            return;
        }
        LocalDate checkIn = guestCheckInPicker.getValue();
        LocalDate checkOut = guestCheckOutPicker.getValue();
        if (checkIn == null || checkOut == null) {
            showMessage("Select check-in and check-out dates.", true);
            return;
        }
        if (checkOut.isBefore(checkIn)) {
            showMessage("Check-out cannot be before check-in.", true);
            return;
        }
        try {
            bookingService.createGuestBooking(user, selected.getRoomNumber(), checkIn, checkOut);
            roomService.markRoomStatus(selected, "OCCUPIED");
            showMessage("Room booked successfully.", false);
            guestCheckInPicker.setValue(null);
            guestCheckOutPicker.setValue(null);
            refreshRooms();
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage(), true);
        } catch (Exception ex) {
            showMessage("Failed to book room.", true);
        }
    }

    private boolean isGuestUser() {
        if (user == null) {
            return false;
        }
        return user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(name -> "GUEST".equalsIgnoreCase(name));
    }
}
