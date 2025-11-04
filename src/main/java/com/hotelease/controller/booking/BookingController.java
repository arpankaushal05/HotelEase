package com.hotelease.controller.booking;

import com.hotelease.model.Booking;
import com.hotelease.model.Role;
import com.hotelease.model.User;
import com.hotelease.service.AuthService;
import com.hotelease.service.BookingService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class BookingController {

    @FXML
    private Label headerLabel;

    @FXML
    private TableView<Booking> bookingTable;

    @FXML
    private TableColumn<Booking, String> guestColumn;

    @FXML
    private TableColumn<Booking, String> roomColumn;

    @FXML
    private TableColumn<Booking, LocalDate> checkInColumn;

    @FXML
    private TableColumn<Booking, LocalDate> checkOutColumn;

    @FXML
    private TableColumn<Booking, String> statusColumn;

    @FXML
    private TextField guestNameField;

    @FXML
    private TextField roomNumberField;

    @FXML
    private DatePicker checkInPicker;

    @FXML
    private DatePicker checkOutPicker;

    @FXML
    private ChoiceBox<String> statusChoiceBox;

    @FXML
    private Label statusLabel;

    private final BookingService bookingService;
    private Stage stage;
    private String stylesheet;
    private User currentUser;
    private AuthService authService;
    private final ObservableList<Booking> bookings = FXCollections.observableArrayList();

    private static final List<String> BOOKING_STATUSES = Arrays.asList(
            "PENDING",
            "CONFIRMED",
            "CHECKED_IN",
            "CHECKED_OUT",
            "CANCELLED"
    );

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @FXML
    private void initialize() {
        setupTable();
        statusChoiceBox.getItems().addAll(BOOKING_STATUSES);
        statusChoiceBox.getSelectionModel().selectFirst();
        statusLabel.setText("");
        bookingTable.setItems(bookings);
        bookingTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> populateForm(newValue));
        refreshBookings();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setStylesheet(String stylesheet) {
        this.stylesheet = stylesheet;
    }

    public void setUser(User user) {
        this.currentUser = user;
        if (user != null) {
            headerLabel.setText("Bookings - " + user.getUsername());
        }
        configureForUser();
        refreshBookings();
    }

    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    @FXML
    private void handleSaveBooking(ActionEvent event) {
        String guestName = guestNameField.getText();
        String roomNumber = roomNumberField.getText();
        LocalDate checkIn = checkInPicker.getValue();
        LocalDate checkOut = checkOutPicker.getValue();
        String status = statusChoiceBox.getValue();

        try {
            Booking selected = bookingTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                if (isGuestUser()) {
                    bookingService.createGuestBooking(currentUser, roomNumber, checkIn, checkOut);
                } else {
                    bookingService.createBooking(guestName, guestName, roomNumber, checkIn, checkOut, status);
                }
            } else {
                if (isGuestUser() && !currentUser.getUsername().equalsIgnoreCase(selected.getGuestUsername())) {
                    showMessage("Guests may only modify their own bookings.", true);
                    return;
                }
                selected.setGuestName(guestName);
                selected.setGuestUsername(isGuestUser() ? currentUser.getUsername() : guestName);
                selected.setRoomNumber(roomNumber);
                selected.setCheckIn(checkIn);
                selected.setCheckOut(checkOut);
                selected.setStatus(isGuestUser() ? selected.getStatus() : status);
                bookingService.createOrUpdateBooking(selected);
            }
            showMessage("Booking saved successfully.", false);
            clearForm();
            refreshBookings();
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage(), true);
        } catch (Exception ex) {
            showMessage("Failed to save booking.", true);
        }
    }

    @FXML
    private void handleNewBooking(ActionEvent event) {
        bookingTable.getSelectionModel().clearSelection();
        clearForm();
        showMessage("", false);
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        refreshBookings();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        navigateToDashboard();
    }

    private void setupTable() {
        guestColumn.setCellValueFactory(new PropertyValueFactory<>("guestUsername"));
        roomColumn.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        checkInColumn.setCellValueFactory(new PropertyValueFactory<>("checkIn"));
        checkOutColumn.setCellValueFactory(new PropertyValueFactory<>("checkOut"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void refreshBookings() {
        bookings.setAll(bookingService.getBookingsForUser(currentUser));
    }

    private void clearForm() {
        if (isGuestUser()) {
            guestNameField.setText(currentUser != null ? currentUser.getUsername() : "");
        } else {
            guestNameField.clear();
        }
        roomNumberField.clear();
        checkInPicker.setValue(null);
        checkOutPicker.setValue(null);
        statusChoiceBox.getSelectionModel().selectFirst();
    }

    private void populateForm(Booking booking) {
        if (booking == null) {
            clearForm();
            return;
        }
        guestNameField.setText(isGuestUser() ? booking.getGuestUsername() : booking.getGuestName());
        roomNumberField.setText(booking.getRoomNumber());
        checkInPicker.setValue(booking.getCheckIn());
        checkOutPicker.setValue(booking.getCheckOut());
        statusChoiceBox.setValue(booking.getStatus());
    }

    private void showMessage(String message, boolean error) {
        statusLabel.getStyleClass().removeAll("error-label", "success-label");
        statusLabel.setText(message == null ? "" : message);
        if (message == null || message.isBlank()) {
            return;
        }
        statusLabel.getStyleClass().add(error ? "error-label" : "success-label");
    }

    private void navigateToDashboard() {
        if (stage == null) {
            showMessage("Stage not available.", true);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/dashboard.fxml"));
            BorderPane root = loader.load();
            com.hotelease.controller.dashboard.DashboardController controller = loader.getController();
            controller.setStage(stage);
            controller.setStylesheet(stylesheet);
            controller.setUser(currentUser);
            controller.setAuthService(authService);
            if (stage.getScene() == null) {
                Scene scene = new Scene(root, 800, 600);
                applyStylesheet(scene);
                stage.setScene(scene);
            } else {
                applyStylesheet(stage.getScene());
                stage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            showMessage("Failed to return to dashboard.", true);
        }
    }

    private void applyStylesheet(Scene scene) {
        if (scene == null || stylesheet == null) {
            return;
        }
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
    }

    private void configureForUser() {
        boolean guest = isGuestUser();
        guestNameField.setDisable(guest);
        statusChoiceBox.setDisable(guest);
        if (guest && currentUser != null) {
            guestNameField.setText(currentUser.getUsername());
            statusChoiceBox.getSelectionModel().select("PENDING");
        }
    }

    private boolean isGuestUser() {
        if (currentUser == null) {
            return false;
        }
        return currentUser.getRoles().stream()
                .map(Role::getName)
                .anyMatch(name -> "GUEST".equalsIgnoreCase(name));
    }
}
