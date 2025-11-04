package com.hotelease.controller.dashboard.modules;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public final class ModuleLauncher {

    private ModuleLauncher() {
    }

    public static void showPlaceholder(String moduleName) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, moduleName + " module coming soon", ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("HotelEase");
        alert.showAndWait();
    }
}
