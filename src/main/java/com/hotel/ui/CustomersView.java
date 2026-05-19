package com.hotel.ui;

import com.hotel.Main;
import com.hotel.domain.Customer;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.UUID;

public class CustomersView {

    private ObservableList<Customer> customerList;

    public VBox build() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f8f8f2;");
        root.getChildren().addAll(buildTopBar(), buildContent());
        return root;
    }

    private HBox buildTopBar() {
        HBox bar = new HBox();
        bar.setPadding(new Insets(16, 20, 16, 20));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color:#ffffff;-fx-border-color:#e0e0e0;-fx-border-width:0 0 1 0;");

        Label title = new Label("Customers");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        title.setTextFill(Color.web("#1e1e2e"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAdd = styledButton("+ Add Customer", "#a6e3a1");
        btnAdd.setOnAction(e -> showAddDialog());

        bar.getChildren().addAll(title, spacer, btnAdd);
        return bar;
    }

    private VBox buildContent() {
        VBox content = new VBox(14);
        content.setPadding(new Insets(20));
        VBox.setVgrow(content, Priority.ALWAYS);

        customerList = FXCollections.observableArrayList(Main.customerRepository.findAll());

        TableView<Customer> table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);
        content.getChildren().add(table);
        return content;
    }

    @SuppressWarnings("unchecked")
    private TableView<Customer> buildTable() {
        TableView<Customer> t = new TableView<>(customerList);
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        t.setStyle("-fx-background-color:white;-fx-background-radius:10;");

        TableColumn<Customer, String> colFirst = new TableColumn<>("First Name");
        colFirst.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFirstName()));

        TableColumn<Customer, String> colLast = new TableColumn<>("Last Name");
        colLast.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getLastName()));

        TableColumn<Customer, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail()));

        TableColumn<Customer, String> colPhone = new TableColumn<>("Phone");
        colPhone.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPhoneNumber()));

        TableColumn<Customer, String> colReservations = new TableColumn<>("Reservations");
        colReservations.setCellValueFactory(d -> {
            long count = Main.reservationService
                .getReservationsByCustomer(d.getValue().getCustomerId()).size();
            return new SimpleStringProperty(String.valueOf(count));
        });
        colReservations.setMaxWidth(110);

        TableColumn<Customer, Void> colActions = new TableColumn<>("Actions");
        colActions.setMaxWidth(100);
        colActions.setCellFactory(col -> new TableCell<>() {
            final Button btnDel = styledButton("Delete", "#f38ba8");
            {
                btnDel.setOnAction(e -> {
                    Customer c = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Delete customer " + c.getFullName() + "?", ButtonType.YES, ButtonType.NO);
                    confirm.showAndWait().ifPresent(b -> {
                        if (b == ButtonType.YES) {
                        Main.customerRepository.delete(c.getCustomerId());
                        customerList.setAll(Main.customerRepository.findAll());
                    }
                    });
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDel);
            }
        });

        t.getColumns().addAll(colFirst, colLast, colEmail, colPhone, colReservations, colActions);
        return t;
    }

    private void showAddDialog() {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("Add Customer");
        dialog.setHeaderText("Enter customer details");

        ButtonType addBtn = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField tfFirst = new TextField(); tfFirst.setPromptText("First name");
        TextField tfLast  = new TextField(); tfLast.setPromptText("Last name");
        TextField tfEmail = new TextField(); tfEmail.setPromptText("email@example.com");
        TextField tfPhone = new TextField(); tfPhone.setPromptText("+1-555-0100");

        grid.addRow(0, new Label("First Name:"), tfFirst);
        grid.addRow(1, new Label("Last Name:"),  tfLast);
        grid.addRow(2, new Label("Email:"),      tfEmail);
        grid.addRow(3, new Label("Phone:"),      tfPhone);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == addBtn) {
                try {
                    Customer c = new Customer(UUID.randomUUID(),
                        tfFirst.getText().trim(), tfLast.getText().trim(),
                        tfEmail.getText().trim(), tfPhone.getText().trim());
                    return Main.customerRepository.save(c);
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK).showAndWait();
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(c -> customerList.setAll(Main.customerRepository.findAll()));
    }

    private Button styledButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:" + color + ";-fx-background-radius:6;" +
                     "-fx-cursor:hand;-fx-font-size:12;-fx-padding:5 12 5 12;");
        return btn;
    }
}
