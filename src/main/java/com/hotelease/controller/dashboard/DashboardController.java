package com.hotelease.controller.dashboard;

import com.hotelease.controller.RegisterEmployeeController;
import com.hotelease.controller.billing.BillingController;
import com.hotelease.controller.booking.BookingController;
import com.hotelease.controller.dashboard.modules.ModuleLauncher;
import com.hotelease.controller.housekeeping.HousekeepingController;
import com.hotelease.controller.help.HelpController;
import com.hotelease.controller.room.RoomController;
import com.hotelease.model.Role;
import com.hotelease.model.User;
import com.hotelease.repository.jdbc.JdbcBillRepository;
import com.hotelease.repository.jdbc.JdbcBookingRepository;
import com.hotelease.repository.jdbc.JdbcHelpChatRepository;
import com.hotelease.repository.jdbc.JdbcRoomRepository;
import com.hotelease.service.AuthService;
import com.hotelease.service.BookingService;
import com.hotelease.service.BillService;
import com.hotelease.service.HelpChatService;
import com.hotelease.service.RoomService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class DashboardController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private FlowPane rolesPane;

    @FXML
    private Button registerEmployeeButton;

    @FXML
    private Button housekeepingButton;

    @FXML
    private Button helpButton;

    @FXML
    private Button bookingsButton;

    @FXML
    private Button roomsButton;

    @FXML
    private Button billingButton;

    private Stage stage;
    private User user;
    private String stylesheet;
    private AuthService authService;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void openHousekeepingModule(ActionEvent event) {
        if (stage == null) {
            ModuleLauncher.showPlaceholder("Stage unavailable");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/housekeeping.fxml"));
            loader.setControllerFactory(param -> {
                if (param == HousekeepingController.class) {
                    RoomService roomService = new RoomService(new JdbcRoomRepository());
                    return new HousekeepingController(roomService);
                }
                try {
                    return param.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to create controller instance", e);
                }
            });
            Pane root = loader.load();
            HousekeepingController controller = loader.getController();
            controller.setStage(stage);
            controller.setStylesheet(stylesheet);
            controller.setAuthService(authService);
            controller.setUser(user);
            if (stage.getScene() == null) {
                Scene scene = new Scene(root, 900, 650);
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
            e.printStackTrace();
            ModuleLauncher.showPlaceholder("Failed to open Housekeeping module: " + e.getMessage());
        }
    }

    public void setStylesheet(String stylesheet) {
        this.stylesheet = stylesheet;
    }

    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    public void setUser(User user) {
        this.user = user;
        welcomeLabel.setText("Welcome, " + user.getUsername() + "!");
        rolesPane.getChildren().clear();
        for (Role role : user.getRoles()) {
            Label roleLabel = new Label(role.getName());
            roleLabel.getStyleClass().add("role-badge");
            rolesPane.getChildren().add(roleLabel);
        }
        boolean isAdmin = user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(name -> "ADMIN".equalsIgnoreCase(name));
        boolean isManager = user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(name -> "MANAGER".equalsIgnoreCase(name));
        if (registerEmployeeButton != null) {
            registerEmployeeButton.setVisible(isAdmin);
            registerEmployeeButton.setManaged(isAdmin);
        }
        boolean isHousekeeping = user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(name -> "HOUSEKEEPING".equalsIgnoreCase(name));
        boolean showHousekeeping = isHousekeeping || isAdmin || isManager;
        if (housekeepingButton != null) {
            housekeepingButton.setVisible(showHousekeeping);
            housekeepingButton.setManaged(showHousekeeping);
            housekeepingButton.setDisable(!showHousekeeping);
        }
        boolean showHelp = !userHasOnlyHousekeepingRole(isHousekeeping, isAdmin, isManager);
        toggleModuleButton(helpButton, showHelp);

        if (isHousekeeping && !isAdmin && !isManager) {
            toggleModuleButton(bookingsButton, false);
            toggleModuleButton(roomsButton, false);
            toggleModuleButton(billingButton, false);
        } else {
            toggleModuleButton(bookingsButton, true);
            toggleModuleButton(roomsButton, true);
            toggleModuleButton(billingButton, true);
        }
    }

    private boolean userHasOnlyHousekeepingRole(boolean isHousekeeping, boolean isAdmin, boolean isManager) {
        if (user == null) {
            return false;
        }
        if (!isHousekeeping) {
            return false;
        }
        if (isAdmin || isManager) {
            return false;
        }
        boolean hasOtherRole = user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(name -> "RECEPTIONIST".equalsIgnoreCase(name));
        return !hasOtherRole;
    }

    @FXML
    private void openBookingModule(ActionEvent event) {
        if (stage == null) {
            ModuleLauncher.showPlaceholder("Stage unavailable");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/booking.fxml"));
            loader.setControllerFactory(param -> {
                if (param == BookingController.class) {
                    BookingService bookingService = new BookingService(new JdbcBookingRepository());
                    return new BookingController(bookingService);
                }
                try {
                    return param.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to create controller instance", e);
                }
            });
            Pane root = loader.load();
            BookingController controller = loader.getController();
            controller.setStage(stage);
            controller.setStylesheet(stylesheet);
            controller.setUser(user);
            if (stage.getScene() == null) {
                Scene scene = new Scene(root, 900, 650);
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
            e.printStackTrace();
            ModuleLauncher.showPlaceholder("Failed to open Booking module: " + e.getMessage());
        }
    }

    @FXML
    private void openRoomModule(ActionEvent event) {
        if (stage == null) {
            ModuleLauncher.showPlaceholder("Stage unavailable");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/rooms.fxml"));
            loader.setControllerFactory(param -> {
                if (param == RoomController.class) {
                    RoomService roomService = new RoomService(new JdbcRoomRepository());
                    BookingService bookingService = new BookingService(new JdbcBookingRepository());
                    return new RoomController(roomService, bookingService);
                }
                try {
                    return param.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to create controller instance", e);
                }
            });
            Pane root = loader.load();
            RoomController controller = loader.getController();
            controller.setStage(stage);
            controller.setStylesheet(stylesheet);
            controller.setUser(user);
            controller.setAuthService(authService);
            if (stage.getScene() == null) {
                Scene scene = new Scene(root, 900, 650);
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
            e.printStackTrace();
            ModuleLauncher.showPlaceholder("Failed to open Room module: " + e.getMessage());
        }
    }

    @FXML
    private void openBillingModule(ActionEvent event) {
        if (stage == null) {
            ModuleLauncher.showPlaceholder("Stage unavailable");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/billing.fxml"));
            loader.setControllerFactory(param -> {
                if (param == BillingController.class) {
                    BillService billService = new BillService(new JdbcBillRepository());
                    return new BillingController(billService);
                }
                try {
                    return param.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to create controller instance", e);
                }
            });
            Pane root = loader.load();
            BillingController controller = loader.getController();
            controller.setStage(stage);
            controller.setStylesheet(stylesheet);
            controller.setAuthService(authService);
            controller.setUser(user);
            if (stage.getScene() == null) {
                Scene scene = new Scene(root, 900, 650);
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
            e.printStackTrace();
            ModuleLauncher.showPlaceholder("Failed to open Billing module: " + e.getMessage());
        }
    }

    @FXML
    private void openHelpModule(ActionEvent event) {
        if (stage == null) {
            ModuleLauncher.showPlaceholder("Stage unavailable");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/help.fxml"));
            loader.setControllerFactory(param -> {
                if (param == HelpController.class) {
                    BookingService bookingService = new BookingService(new JdbcBookingRepository());
                    HelpChatService helpChatService = new HelpChatService(new JdbcHelpChatRepository(), new JdbcRoomRepository());
                    return new HelpController(bookingService, helpChatService);
                }
                try {
                    return param.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to create controller instance", e);
                }
            });
            Pane root = loader.load();
            HelpController controller = loader.getController();
            controller.setStage(stage);
            controller.setStylesheet(stylesheet);
            controller.setAuthService(authService);
            controller.setUser(user);
            if (stage.getScene() == null) {
                Scene scene = new Scene(root, 900, 650);
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
            e.printStackTrace();
            ModuleLauncher.showPlaceholder("Failed to open Help module: " + e.getMessage());
        }
    }

    @FXML
    private void openRegisterEmployee(ActionEvent event) {
        if (!isAdminUser()) {
            ModuleLauncher.showPlaceholder("Only administrators can register employees.");
            return;
        }
        if (stage == null) {
            ModuleLauncher.showPlaceholder("Stage unavailable");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/register-employee.fxml"));
            loader.setControllerFactory(param -> {
                if (param == RegisterEmployeeController.class) {
                    return new RegisterEmployeeController(authService);
                }
                try {
                    return param.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to create controller instance", e);
                }
            });
            Pane root = loader.load();
            RegisterEmployeeController controller = loader.getController();

            Stage dialog = new Stage();
            dialog.initOwner(stage);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle("Register Employee");
            Scene scene = new Scene(root, 420, 360);
            if (stylesheet != null) {
                scene.getStylesheets().add(stylesheet);
            }
            dialog.setScene(scene);
            controller.setDialogStage(dialog);
            controller.initializeForm();
            dialog.showAndWait();
        } catch (IOException e) {
            ModuleLauncher.showPlaceholder("Failed to open employee registration: " + e.getMessage());
        }
    }

    private boolean isAdminUser() {
        return user != null && user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(name -> "ADMIN".equalsIgnoreCase(name));
    }

    private void toggleModuleButton(Button button, boolean visible) {
        if (button == null) {
            return;
        }
        button.setVisible(visible);
        button.setManaged(visible);
        button.setDisable(!visible);
    }
}
