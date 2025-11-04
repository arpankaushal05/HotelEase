package com.hotelease.app;

import com.hotelease.config.DatabaseConfig;
import com.hotelease.controller.AdminLoginController;
import com.hotelease.controller.EmployeeLoginController;
import com.hotelease.controller.GuestLoginController;
import com.hotelease.controller.GuestRegisterController;
import com.hotelease.controller.LaunchController;
import com.hotelease.repository.jdbc.JdbcRoleRepository;
import com.hotelease.repository.jdbc.JdbcUserRepository;
import com.hotelease.service.AuthService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Entry point for the HotelEase desktop application.
 */
public class HotelEaseApplication extends Application {

    private AuthService authService;

    @Override
    public void init() {
        DatabaseConfig.startTcpServer();
        DatabaseConfig.initializeSchema();

        JdbcRoleRepository roleRepository = new JdbcRoleRepository();
        JdbcUserRepository userRepository = new JdbcUserRepository();
        authService = new AuthService(userRepository, roleRepository);
        authService.initializeDefaults();
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            String stylesheet = Objects.requireNonNull(getClass().getResource("/css/application.css"),
                    "Stylesheet /css/application.css not found").toExternalForm();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/launch.fxml"));
            loader.setControllerFactory(param -> {
                if (param == LaunchController.class) {
                    LaunchController controller = new LaunchController(authService);
                    controller.setStage(primaryStage);
                    controller.setStylesheet(stylesheet);
                    return controller;
                }
                if (param == GuestLoginController.class) {
                    return new GuestLoginController(authService);
                }
                if (param == GuestRegisterController.class) {
                    return new GuestRegisterController(authService);
                }
                if (param == AdminLoginController.class) {
                    return new AdminLoginController(authService);
                }
                if (param == EmployeeLoginController.class) {
                    return new EmployeeLoginController(authService);
                }
                try {
                    return param.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to create controller instance", e);
                }
            });
            Parent root = loader.load();
            LaunchController launchController = loader.getController();
            launchController.setStage(primaryStage);
            launchController.setStylesheet(stylesheet);

            Scene scene = new Scene(root, 800, 600);
            scene.getStylesheets().add(stylesheet);
            primaryStage.setTitle("HotelEase");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load launch view", e);
        }
    }

    @Override
    public void stop() {
        DatabaseConfig.stopTcpServer();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
