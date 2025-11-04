package com.hotelease.controller;

import com.hotelease.context.SessionContext;
import com.hotelease.controller.dashboard.DashboardController;
import com.hotelease.model.User;
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
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public abstract class BaseLoginController {

    @FXML
    protected TextField usernameField;

    @FXML
    protected PasswordField passwordField;

    @FXML
    protected Label statusLabel;

    protected final AuthService authService;
    protected Stage stage;
    protected String stylesheet;
    private MediaPlayer backgroundPlayer;
    private ImageView fallbackImage;

    protected BaseLoginController(AuthService authService) {
        this.authService = authService;
    }

    @FXML
    protected void initialize() {
        if (statusLabel != null) {
            statusLabel.setText("");
        }
        initializeBackgroundVideo();
    }

    @FXML
    protected MediaView backgroundVideo;

    @FXML
    protected StackPane rootPane;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setStylesheet(String stylesheet) {
        this.stylesheet = stylesheet;
    }

    public void prefillUsername(String username) {
        if (usernameField != null && username != null) {
            usernameField.setText(username);
        }
    }

    @FXML
    protected void handleLogin(ActionEvent event) {
        Optional<String> validationError = validateBeforeLogin();
        if (validationError.isPresent()) {
            showMessage(validationError.get(), true);
            return;
        }

        String username = usernameField != null ? usernameField.getText() : null;
        String password = passwordField != null ? passwordField.getText() : null;
        Optional<User> authenticatedUser = authService.authenticate(username, password);
        if (authenticatedUser.isEmpty()) {
            showMessage("Invalid credentials or inactive user.", true);
            return;
        }

        User user = authenticatedUser.get();
        if (!isRoleAllowed(user)) {
            showMessage(getRoleDeniedMessage(), true);
            return;
        }

        SessionContext.getInstance().setCurrentUser(user);
        showMessage("", false);
        afterSuccessfulLogin(user);
    }

    protected Optional<String> validateBeforeLogin() {
        return Optional.empty();
    }

    protected void afterSuccessfulLogin(User user) {
        navigateToDashboard(user);
    }

    protected boolean isRoleAllowed(User user) {
        return true;
    }

    protected String getRoleDeniedMessage() {
        return "You are not authorized to access this portal.";
    }

    protected void navigateToDashboard(User user) {
        if (stage == null) {
            showMessage("Application stage not available.", true);
            return;
        }
        stopBackgroundVideo();
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
                applyStylesheet(scene);
                stage.setScene(scene);
            } else {
                applyStylesheet(stage.getScene());
                stage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            showMessage("Failed to open dashboard.", true);
        }
    }

    @FXML
    protected void backToLaunch(ActionEvent event) {
        navigateToLaunch();
    }

    protected void navigateToLaunch() {
        if (stage == null) {
            showMessage("Application stage not available.", true);
            return;
        }
        stopBackgroundVideo();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/launch.fxml"));
            loader.setControllerFactory(param -> {
                if (param == LaunchController.class) {
                    LaunchController controller = new LaunchController(authService);
                    controller.setStage(stage);
                    controller.setStylesheet(stylesheet);
                    return controller;
                }
                if (param == GuestLoginController.class) {
                    return new GuestLoginController(authService);
                }
                if (param == AdminLoginController.class) {
                    return new AdminLoginController(authService);
                }
                if (param == EmployeeLoginController.class) {
                    return new EmployeeLoginController(authService);
                }
                if (param == GuestRegisterController.class) {
                    return new GuestRegisterController(authService);
                }
                try {
                    return param.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to create controller instance", e);
                }
            });
            Parent root = loader.load();
            if (stage.getScene() == null) {
                Scene scene = new Scene(root, 800, 600);
                applyStylesheet(scene);
                stage.setScene(scene);
            } else {
                applyStylesheet(stage.getScene());
                stage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            showMessage("Failed to open launch screen.", true);
        }
    }

    public void showMessage(String message, boolean error) {
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

    protected void applyStylesheet(Scene scene) {
        if (scene == null || stylesheet == null) {
            return;
        }
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
    }

    private void initializeBackgroundVideo() {
        if (backgroundVideo == null || rootPane == null) {
            return;
        }
        Runnable setupVideo = () -> {
            try {
                removeFallbackBackground();
                Media media = new Media(getRequiredResource("/bg/login.mp4"));
                backgroundPlayer = new MediaPlayer(media);
                backgroundPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                backgroundPlayer.setMute(true);
                backgroundVideo.setMediaPlayer(backgroundPlayer);
                backgroundVideo.fitWidthProperty().bind(rootPane.widthProperty());
                backgroundVideo.fitHeightProperty().bind(rootPane.heightProperty());
                backgroundPlayer.setOnReady(() -> {
                    backgroundPlayer.play();
                    removeFallbackBackground();
                });
                backgroundPlayer.setOnError(() -> {
                    addFallbackBackground();
                    stopBackgroundVideo();
                });
                backgroundPlayer.play();
            } catch (Exception ex) {
                addFallbackBackground();
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

    private void addFallbackBackground() {
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

    private void removeFallbackBackground() {
        if (fallbackImage != null) {
            rootPane.getChildren().remove(fallbackImage);
        }
    }

    private String getRequiredResource(String path) {
        return java.util.Objects.requireNonNull(getClass().getResource(path),
                "Missing resource " + path).toExternalForm();
    }

    private void stopBackgroundVideo() {
        if (backgroundPlayer != null) {
            backgroundPlayer.stop();
            backgroundPlayer.dispose();
            backgroundPlayer = null;
        }
        if (backgroundVideo != null) {
            backgroundVideo.setMediaPlayer(null);
        }
        removeFallbackBackground();
    }
}
