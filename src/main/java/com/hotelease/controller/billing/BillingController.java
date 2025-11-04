package com.hotelease.controller.billing;

import com.hotelease.controller.dashboard.DashboardController;
import com.hotelease.model.Bill;
import com.hotelease.model.Role;
import com.hotelease.model.User;
import com.hotelease.service.AuthService;
import com.hotelease.service.BillService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class BillingController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @FXML
    private TableView<Bill> billTable;

    @FXML
    private TableColumn<Bill, String> invoiceColumn;

    @FXML
    private TableColumn<Bill, String> guestColumn;

    @FXML
    private TableColumn<Bill, String> guestPhoneColumn;

    @FXML
    private TableColumn<Bill, BigDecimal> amountColumn;

    @FXML
    private TableColumn<Bill, String> statusColumn;

    @FXML
    private TableColumn<Bill, String> issuedColumn;

    @FXML
    private TableColumn<Bill, String> dueColumn;

    @FXML
    private ComboBox<String> statusFilterCombo;

    @FXML
    private Button markPaidButton;

    @FXML
    private Button refreshButton;

    @FXML
    private Button backButton;

    @FXML
    private Label statusLabel;

    private final ObservableList<Bill> bills = FXCollections.observableArrayList();
    private final BillService billService;
    private Stage stage;
    private String stylesheet;
    private User user;
    private AuthService authService;

    public BillingController(BillService billService) {
        this.billService = billService;
    }

    @FXML
    private void initialize() {
        invoiceColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getInvoiceNumber()));
        guestColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getGuestUsername()));
        if (guestPhoneColumn != null) {
            guestPhoneColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                    cell.getValue().getGuestPhone() == null ? "-" : cell.getValue().getGuestPhone()));
        }
        amountColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getAmount()));
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus()));
        issuedColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatDate(cell.getValue().getIssuedDate())));
        dueColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatDate(cell.getValue().getDueDate())));

        statusFilterCombo.setItems(FXCollections.observableArrayList("ALL", "PENDING", "PAID"));
        statusFilterCombo.getSelectionModel().selectFirst();

        billTable.setItems(bills);
        refreshBills();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setStylesheet(String stylesheet) {
        this.stylesheet = stylesheet;
    }

    public void setUser(User user) {
        this.user = user;
        boolean guest = user != null && user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(name -> "GUEST".equalsIgnoreCase(name));
        if (markPaidButton != null) {
            markPaidButton.setDisable(guest);
            markPaidButton.setVisible(!guest);
            markPaidButton.setManaged(!guest);
        }
        if (statusFilterCombo != null) {
            refreshBills();
        }
    }

    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        refreshBills();
    }

    @FXML
    private void handleMarkPaid(ActionEvent event) {
        if (user != null && user.getRoles().stream().map(Role::getName).anyMatch(name -> "GUEST".equalsIgnoreCase(name))) {
            showMessage("Guests cannot mark bills as paid.", true);
            return;
        }
        Bill selected = billTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Select a bill to mark as paid.", true);
            return;
        }
        if ("PAID".equalsIgnoreCase(selected.getStatus())) {
            showMessage("Bill is already marked as paid.", false);
            return;
        }
        try {
            billService.markAsPaid(selected);
            showMessage("Bill marked as paid.", false);
            refreshBills();
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage(), true);
        } catch (Exception ex) {
            showMessage("Failed to update bill.", true);
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

    private void refreshBills() {
        String filter = statusFilterCombo.getSelectionModel().getSelectedItem();
        bills.setAll(billService.getBillsForUser(user, filter));
        showMessage("", false);
    }

    private void showMessage(String message, boolean error) {
        statusLabel.getStyleClass().removeAll("error-label", "success-label");
        statusLabel.setText(message == null ? "" : message);
        if (message == null || message.isBlank()) {
            return;
        }
        statusLabel.getStyleClass().add(error ? "error-label" : "success-label");
    }

    private String formatDate(java.time.LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }
}
