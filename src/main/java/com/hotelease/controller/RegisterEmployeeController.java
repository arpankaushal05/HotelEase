package com.hotelease.controller;

import com.hotelease.service.AuthService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.List;

public class RegisterEmployeeController {

    private static final List<String> EMPLOYEE_ROLES = List.of("RECEPTIONIST", "HOUSEKEEPING", "MANAGER");

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField phoneField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private ChoiceBox<String> roleChoiceBox;

    @FXML
    private Label statusLabel;

    private final AuthService authService;
    private final ObservableList<String> roles = FXCollections.observableArrayList();
    private Stage dialogStage;

    public RegisterEmployeeController(AuthService authService) {
        this.authService = authService;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void initializeForm() {
        statusLabel.setText("");
        roles.setAll(EMPLOYEE_ROLES);
        roleChoiceBox.setItems(roles);
        if (!roles.isEmpty()) {
            roleChoiceBox.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String selectedRole = roleChoiceBox.getSelectionModel().getSelectedItem();

        if (password == null || !password.equals(confirmPassword)) {
            showError("Passwords do not match.");
            return;
        }

        try {
            authService.registerEmployee(username, email, phone, password, selectedRole);
            showSuccess("Employee registered successfully.");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Failed to register employee.");
        }
    }

    @FXML
    private void handleClose() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    private void showError(String message) {
        statusLabel.getStyleClass().removeAll("success-label", "error-label");
        statusLabel.getStyleClass().add("error-label");
        statusLabel.setText(message);
    }

    private void showSuccess(String message) {
        statusLabel.getStyleClass().removeAll("success-label", "error-label");
        statusLabel.getStyleClass().add("success-label");
        statusLabel.setText(message);
    }
}
