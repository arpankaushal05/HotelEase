package com.hotelease.controller;

import com.hotelease.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;

import java.io.IOException;

public class GuestRegisterController {

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
    private Label statusLabel;

    @FXML
    private MediaView backgroundVideo;

    @FXML
    private StackPane rootPane;

    private final AuthService authService;
    private Stage stage;
    private String stylesheet;
    private String loginPrefill;
    private MediaPlayer mediaPlayer;
    private ImageView fallbackImage;

    public GuestRegisterController(AuthService authService) {
        this.authService = authService;
    }

    @FXML
    private void initialize() {
        statusLabel.setText("");
        initializeBackground();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setStylesheet(String stylesheet) {
        this.stylesheet = stylesheet;
    }

    public void setLoginPrefill(String loginPrefill) {
        this.loginPrefill = loginPrefill;
        if (loginPrefill != null && !loginPrefill.isBlank()) {
            usernameField.setText(loginPrefill);
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        String username = usernameField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (password == null || !password.equals(confirmPassword)) {
            showMessage("Passwords do not match.", true);
            return;
        }

        try {
            authService.registerUser(username, email, phone, password);
            navigateToLogin(username, "Registration successful. You can now log in.", false);
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage(), true);
        } catch (Exception ex) {
            showMessage("Failed to register user.", true);
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        navigateToLogin(loginPrefill, "", false);
    }

    private void navigateToLogin(String prefill, String message, boolean error) {
        if (stage == null) {
            showMessage("Stage not available.", true);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/guest-login.fxml"));
            loader.setControllerFactory(param -> {
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
            GuestLoginController controller = loader.getController();
            controller.setStage(stage);
            controller.setStylesheet(stylesheet);
            controller.prefillUsername(prefill);
            controller.showMessage(message, error);

            if (stage.getScene() == null) {
                Scene scene = new Scene(root, 800, 600);
                applyStylesheet(scene);
                stage.setScene(scene);
            } else {
                applyStylesheet(stage.getScene());
                stage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            showMessage("Failed to open login view.", true);
        }
    }

    private void showMessage(String message, boolean error) {
        statusLabel.getStyleClass().removeAll("error-label", "success-label");
        statusLabel.setText(message == null ? "" : message);
        if (message == null || message.isBlank()) {
            return;
        }
        statusLabel.getStyleClass().add(error ? "error-label" : "success-label");
    }

    private void applyStylesheet(Scene scene) {
        if (scene == null || stylesheet == null) {
            return;
        }
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
    }

    private void initializeBackground() {
        if (backgroundVideo == null || rootPane == null) {
            return;
        }
        Runnable setupVideo = () -> {
            try {
                removeFallback();
                Media media = new Media(getRequiredResource("/bg/login.mp4"));
                mediaPlayer = new MediaPlayer(media);
                mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                mediaPlayer.setMute(true);
                backgroundVideo.setMediaPlayer(mediaPlayer);
                backgroundVideo.fitWidthProperty().bind(rootPane.widthProperty());
                backgroundVideo.fitHeightProperty().bind(rootPane.heightProperty());
                mediaPlayer.setOnReady(() -> {
                    mediaPlayer.play();
                    removeFallback();
                });
                mediaPlayer.setOnError(() -> {
                    addFallback();
                    stopBackground();
                });
                mediaPlayer.play();
            } catch (Exception ex) {
                addFallback();
            }
        };

        if (rootPane.getScene() == null) {
            rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    setupVideo.run();
                }
            });
        } else {
            setupVideo.run();
        }
    }

    private void addFallback() {
        if (rootPane == null) {
            return;
        }
        if (fallbackImage == null) {
            fallbackImage = new ImageView(new Image(getRequiredResource("/bg/rest.jpg")));
            fallbackImage.setPreserveRatio(false);
            fallbackImage.fitWidthProperty().bind(rootPane.widthProperty());
            fallbackImage.fitHeightProperty().bind(rootPane.heightProperty());
        }
        if (!rootPane.getChildren().contains(fallbackImage)) {
            rootPane.getChildren().add(0, fallbackImage);
        }
    }

    private void removeFallback() {
        if (fallbackImage != null) {
            rootPane.getChildren().remove(fallbackImage);
        }
    }

    private void stopBackground() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }

    private String getRequiredResource(String path) {
        return java.util.Objects.requireNonNull(getClass().getResource(path),
                "Missing resource " + path).toExternalForm();
    }
}
