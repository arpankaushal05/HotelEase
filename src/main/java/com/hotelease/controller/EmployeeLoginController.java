package com.hotelease.controller;

import com.hotelease.model.Role;
import com.hotelease.model.User;
import com.hotelease.service.AuthService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class EmployeeLoginController extends BaseLoginController {

    private static final List<RoleOption> ROLE_OPTIONS = List.of(
            new RoleOption("Receptionist", "RECEPTIONIST"),
            new RoleOption("Housekeeping", "HOUSEKEEPING"),
            new RoleOption("Manager", "MANAGER")
    );

    @FXML
    private ComboBox<String> roleComboBox;

    private final ObservableList<String> displayRoles = FXCollections.observableArrayList();
    private final Map<String, String> displayToCode = ROLE_OPTIONS.stream()
            .collect(Collectors.toMap(RoleOption::displayName, RoleOption::roleCode));

    public EmployeeLoginController(AuthService authService) {
        super(authService);
    }

    @Override
    protected void initialize() {
        super.initialize();
        displayRoles.setAll(displayToCode.keySet());
        if (roleComboBox != null) {
            roleComboBox.setItems(displayRoles);
            roleComboBox.getSelectionModel().clearSelection();
            roleComboBox.setValue(null);
        }
    }

    @Override
    protected Optional<String> validateBeforeLogin() {
        if (roleComboBox == null || roleComboBox.getSelectionModel().getSelectedItem() == null) {
            return Optional.of("Please select your role before logging in.");
        }
        return Optional.empty();
    }

    @Override
    protected boolean isRoleAllowed(User user) {
        if (roleComboBox == null) {
            return false;
        }
        String selectedDisplay = roleComboBox.getSelectionModel().getSelectedItem();
        if (selectedDisplay == null) {
            return false;
        }
        String expectedRole = displayToCode.get(selectedDisplay);
        if (expectedRole == null) {
            return false;
        }
        return user.getRoles().stream()
                .map(Role::getName)
                .map(String::toUpperCase)
                .anyMatch(expectedRole::equalsIgnoreCase);
    }

    @Override
    protected String getRoleDeniedMessage() {
        return "You are not authorized for the selected role.";
    }

    private record RoleOption(String displayName, String roleCode) {
    }
}
