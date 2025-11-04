package com.hotelease.controller;

import com.hotelease.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class LaunchController {

    private final AuthService authService;
    private Stage stage;
    private String stylesheet;
    private MediaPlayer mediaPlayer;
    private ImageView fallbackImage;

    @FXML
    private MediaView backgroundVideo;

    @FXML
    private StackPane rootPane;

    public LaunchController(AuthService authService) {
        this.authService = Objects.requireNonNull(authService, "authService");
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setStylesheet(String stylesheet) {
        this.stylesheet = stylesheet;
    }

    @FXML
    private void initialize() {
        initializeBackground();
    }

    @FXML
    private void openGuestLogin(ActionEvent event) {
        navigateTo("/view/guest-login.fxml", controller -> {
            if (controller instanceof GuestLoginController guestLoginController) {
                guestLoginController.setStage(stage);
                guestLoginController.setStylesheet(stylesheet);
            }
        });
    }

    @FXML
    private void openAdminLogin(ActionEvent event) {
        navigateTo("/view/admin-login.fxml", controller -> {
            if (controller instanceof AdminLoginController adminLoginController) {
                adminLoginController.setStage(stage);
                adminLoginController.setStylesheet(stylesheet);
            }
        });
    }

    @FXML
    private void openEmployeeLogin(ActionEvent event) {
        navigateTo("/view/employee-login.fxml", controller -> {
            if (controller instanceof EmployeeLoginController employeeLoginController) {
                employeeLoginController.setStage(stage);
                employeeLoginController.setStylesheet(stylesheet);
            }
        });
    }

    private void initializeBackground() {
        if (backgroundVideo == null || rootPane == null) {
            return;
        }
        Runnable setupVideo = () -> {
            try {
                removeFallbackImage();
                Media media = new Media(Objects.requireNonNull(getClass().getResource("/bg/login.mp4"),
                        "Missing background video /bg/login.mp4").toExternalForm());
                mediaPlayer = new MediaPlayer(media);
                mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                mediaPlayer.setMute(true);
                backgroundVideo.setMediaPlayer(mediaPlayer);
                backgroundVideo.fitWidthProperty().bind(rootPane.widthProperty());
                backgroundVideo.fitHeightProperty().bind(rootPane.heightProperty());
                mediaPlayer.setOnReady(() -> {
                    mediaPlayer.play();
                    removeFallbackImage();
                });
                mediaPlayer.setOnError(() -> {
                    addFallbackImage();
                    stopMediaPlayer();
                });
                mediaPlayer.play();
            } catch (Exception ex) {
                addFallbackImage();
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

    private void addFallbackImage() {
        if (rootPane == null) {
            return;
        }
        if (fallbackImage == null) {
            fallbackImage = new ImageView(new Image(Objects.requireNonNull(
                    getClass().getResource("/bg/rest.jpg"),
                    "Missing fallback image /bg/rest.jpg").toExternalForm()));
            fallbackImage.setPreserveRatio(false);
            fallbackImage.fitWidthProperty().bind(rootPane.widthProperty());
            fallbackImage.fitHeightProperty().bind(rootPane.heightProperty());
        }
        if (!rootPane.getChildren().contains(fallbackImage)) {
            rootPane.getChildren().add(0, fallbackImage);
        }
    }

    private void removeFallbackImage() {
        if (fallbackImage != null) {
            rootPane.getChildren().remove(fallbackImage);
        }
    }

    private void stopMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        if (backgroundVideo != null) {
            backgroundVideo.setMediaPlayer(null);
        }
    }

    private void navigateTo(String fxmlPath, java.util.function.Consumer<Object> controllerConfigurator) {
        if (stage == null) {
            throw new IllegalStateException("Primary stage not initialized in LaunchController");
        }
        try {
            stopMediaPlayer();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(this::createController);
            Parent root = loader.load();
            Object controller = loader.getController();
            controllerConfigurator.accept(controller);
            if (stage.getScene() == null) {
                Scene scene = new Scene(root, 800, 600);
                applyStylesheet(scene);
                stage.setScene(scene);
            } else {
                applyStylesheet(stage.getScene());
                stage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open view: " + fxmlPath, e);
        }
    }

    private Object createController(Class<?> controllerClass) {
        if (controllerClass == LaunchController.class) {
            return this;
        }
        if (controllerClass == GuestLoginController.class) {
            return new GuestLoginController(authService);
        }
        if (controllerClass == AdminLoginController.class) {
            return new AdminLoginController(authService);
        }
        if (controllerClass == EmployeeLoginController.class) {
            return new EmployeeLoginController(authService);
        }
        if (controllerClass == GuestRegisterController.class) {
            return new GuestRegisterController(authService);
        }
        try {
            return controllerClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to instantiate controller " + controllerClass.getName(), e);
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
}
