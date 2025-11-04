package com.hotelease.controller.housekeeping;

import com.hotelease.controller.dashboard.DashboardController;
import com.hotelease.model.Role;
import com.hotelease.model.Room;
import com.hotelease.model.User;
import com.hotelease.service.AuthService;
import com.hotelease.service.RoomService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;

public class HousekeepingController {

    @FXML
    private TableView<Room> roomTable;

    @FXML
    private TableColumn<Room, String> numberColumn;

    @FXML
    private TableColumn<Room, String> typeColumn;

    @FXML
    private TableColumn<Room, String> statusColumn;

    @FXML
    private Label statusLabel;

    @FXML
    private Button markMaintenanceButton;

    @FXML
    private Button markAvailableButton;

    private final ObservableList<Room> rooms = FXCollections.observableArrayList();
    private final RoomService roomService;
    private Stage stage;
    private String stylesheet;
    private User user;
    private AuthService authService;

    public HousekeepingController(RoomService roomService) {
        this.roomService = roomService;
    }

    @FXML
    private void initialize() {
        numberColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getRoomNumber()));
        typeColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getRoomType()));
        statusColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getStatus()));

        roomTable.setItems(rooms);
        roomTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> updateActionButtons(newSel));
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
        boolean housekeeping = user != null && user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(name -> "HOUSEKEEPING".equalsIgnoreCase(name));
        boolean adminOrManager = user != null && user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(name -> "ADMIN".equalsIgnoreCase(name) || "MANAGER".equalsIgnoreCase(name));
        boolean allowActions = housekeeping || adminOrManager;
        if (!allowActions) {
            if (markMaintenanceButton != null) {
                markMaintenanceButton.setDisable(true);
                markMaintenanceButton.setManaged(false);
                markMaintenanceButton.setVisible(false);
            }
            if (markAvailableButton != null) {
                markAvailableButton.setDisable(true);
                markAvailableButton.setManaged(false);
                markAvailableButton.setVisible(false);
            }
        }
        updateActionButtons(allowActions ? roomTable != null ? roomTable.getSelectionModel().getSelectedItem() : null : null);
    }

    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    @FXML
    private void handleMarkMaintenance(ActionEvent event) {
        Room selected = roomTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Select a room to update.", true);
            return;
        }
        if ("MAINTENANCE".equalsIgnoreCase(selected.getStatus())) {
            showMessage("Room is already in maintenance.", false);
            return;
        }
        try {
            roomService.markRoomStatus(selected, "MAINTENANCE");
            showMessage("Room marked for maintenance.", false);
            refreshRooms();
        } catch (Exception ex) {
            showMessage("Failed to update room status.", true);
        }
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        refreshRooms();
        showMessage("Rooms refreshed.", false);
    }

    @FXML
    private void handleMarkAvailable(ActionEvent event) {
        Room selected = roomTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Select a room to update.", true);
            return;
        }
        if ("AVAILABLE".equalsIgnoreCase(selected.getStatus())) {
            showMessage("Room is already available.", false);
            return;
        }
        try {
            roomService.markRoomStatus(selected, "AVAILABLE");
            showMessage("Room marked available.", false);
            refreshRooms();
        } catch (Exception ex) {
            showMessage("Failed to update room status.", true);
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

    private void refreshRooms() {
        rooms.setAll(roomService.getAllRooms());
        if (roomTable != null) {
            roomTable.getSelectionModel().clearSelection();
        }
        updateActionButtons(null);
    }

    private void showMessage(String message, boolean error) {
        if (statusLabel == null) {
            return;
        }
        statusLabel.getStyleClass().removeAll("error-label", "success-label");
        statusLabel.setText(message == null ? "" : message);
        if (message == null || message.isBlank()) {
            return;
        }
        statusLabel.getStyleClass().add(error ? "error-label" : "success-label");
    }

    private void updateActionButtons(Room room) {
        if (markMaintenanceButton == null || markAvailableButton == null) {
            return;
        }
        if (room == null) {
            markMaintenanceButton.setDisable(true);
            markAvailableButton.setDisable(true);
            return;
        }
        String status = room.getStatus() == null ? "" : room.getStatus().toUpperCase();
        markMaintenanceButton.setDisable("MAINTENANCE".equals(status));
        markAvailableButton.setDisable("AVAILABLE".equals(status));
    }
}
