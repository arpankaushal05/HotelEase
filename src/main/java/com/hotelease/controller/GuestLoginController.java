package com.hotelease.controller;

import com.hotelease.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

public class GuestLoginController extends BaseLoginController {

    public GuestLoginController(AuthService authService) {
        super(authService);
    }

    @FXML
    private void openRegistration(ActionEvent event) {
        if (stage == null) {
            showMessage("Application stage not available.", true);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/register.fxml"));
            loader.setControllerFactory(param -> {
                if (param == GuestRegisterController.class) {
                    return new GuestRegisterController(authService);
                }
                if (param == GuestLoginController.class) {
                    return new GuestLoginController(authService);
                }
                if (param == LaunchController.class) {
                    LaunchController controller = new LaunchController(authService);
                    controller.setStage(stage);
                    controller.setStylesheet(stylesheet);
                    return controller;
                }
                try {
                    return param.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to create controller instance", e);
                }
            });
            Parent root = loader.load();
            GuestRegisterController controller = loader.getController();
            controller.setStage(stage);
            controller.setStylesheet(stylesheet);
            controller.setLoginPrefill(usernameField != null ? usernameField.getText() : null);

            if (stage.getScene() == null) {
                Scene scene = new Scene(root, 800, 600);
                applyStylesheet(scene);
                stage.setScene(scene);
            } else {
                applyStylesheet(stage.getScene());
                stage.getScene().setRoot(root);
            }
        } catch (Exception e) {
            showMessage("Failed to open registration.", true);
        }
    }
}
