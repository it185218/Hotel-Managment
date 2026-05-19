package com.hotel.ui;

import com.hotel.Main;
import com.hotel.domain.Customer;
import com.hotel.domain.Reservation;
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

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

public class CustomersView {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yy");
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
        title.setStyle("-fx-text-fill:#1e1e2e;");

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

        customerList = FXCollections.observableArrayList(
            Main.customerRepository.findAll());

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
        colFirst.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getFirstName()));

        TableColumn<Customer, String> colLast = new TableColumn<>("Last Name");
        colLast.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getLastName()));

        TableColumn<Customer, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getEmail()));

        TableColumn<Customer, String> colPhone = new TableColumn<>("Phone");
        colPhone.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getPhoneNumber()));

        TableColumn<Customer, String> colRes = new TableColumn<>("Bookings");
        colRes.setCellValueFactory(d -> {
            long count = Main.reservationService
                .getReservationsByCustomer(d.getValue().getCustomerId()).size();
            return new SimpleStringProperty(String.valueOf(count));
        });
        colRes.setMaxWidth(80);

        TableColumn<Customer, String> colSpent = new TableColumn<>("Total Spent");
        colSpent.setCellValueFactory(d -> {
            double total = Main.reservationService
                .getReservationsByCustomer(d.getValue().getCustomerId()).stream()
                .filter(r -> !r.getStatus().name().equals("CANCELLED"))
                .mapToDouble(r -> {
                    long n = ChronoUnit.DAYS.between(
                        r.getCheckInDate(), r.getCheckOutDate());
                    return n * r.getRoom().getPricePerNight();
                }).sum();
            return new SimpleStringProperty(String.format("$%.0f", total));
        });
        colSpent.setMaxWidth(100);

        TableColumn<Customer, Void> colActions = new TableColumn<>("Actions");
        colActions.setMaxWidth(160);
        colActions.setCellFactory(col -> new TableCell<>() {
            final Button btnInfo   = styledButton("Info",   "#89b4fa");
            final Button btnDelete = styledButton("Delete", "#f38ba8");
            final HBox   box       = new HBox(6, btnInfo, btnDelete);
            {
                box.setAlignment(Pos.CENTER);
                btnInfo.setOnAction(e -> {
                    Customer c = getTableView().getItems().get(getIndex());
                    showCustomerInfoDialog(c);
                });
                btnDelete.setOnAction(e -> {
                    Customer c = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Delete customer " + c.getFullName() + "?",
                        ButtonType.YES, ButtonType.NO);
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
                setGraphic(empty ? null : box);
            }
        });

        t.getColumns().addAll(
            colFirst, colLast, colEmail, colPhone, colRes, colSpent, colActions);
        return t;
    }

    // ── Customer Info Dialog ───────────────────────────────────────────────────
    private void showCustomerInfoDialog(Customer customer) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(customer.getFullName() + " - Customer Profile");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(16);
        content.setPadding(new Insets(20));
        content.setPrefWidth(620);

        // Customer header card
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16));
        header.setStyle(
            "-fx-background-color:#f0fff4;-fx-background-radius:10;" +
            "-fx-border-color:#a6e3a1;-fx-border-radius:10;-fx-border-width:0 0 0 4;"
        );

        // Avatar circle
        Label avatar = new Label(
            String.valueOf(customer.getFirstName().charAt(0)).toUpperCase());
        avatar.setMinSize(52, 52);
        avatar.setMaxSize(52, 52);
        avatar.setAlignment(Pos.CENTER);
        avatar.setFont(Font.font("System", FontWeight.BOLD, 22));
        avatar.setStyle(
            "-fx-background-color:#a6e3a1;-fx-background-radius:26;-fx-text-fill:#1e1e2e;");

        VBox info = new VBox(5);
        Label nameLabel = new Label(customer.getFullName());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 17));
        nameLabel.setStyle("-fx-text-fill:#1e1e2e;");

        Label emailLabel = new Label("Email:  " + customer.getEmail());
        emailLabel.setFont(Font.font("System", 12));
        emailLabel.setStyle("-fx-text-fill:#6c7086;");

        Label phoneLabel = new Label("Phone:  " +
            (customer.getPhoneNumber().isBlank() ? "—" : customer.getPhoneNumber()));
        phoneLabel.setFont(Font.font("System", 12));
        phoneLabel.setStyle("-fx-text-fill:#6c7086;");

        info.getChildren().addAll(nameLabel, emailLabel, phoneLabel);
        header.getChildren().addAll(avatar, info);

        // Reservation history
        List<Reservation> reservations = Main.reservationService
            .getReservationsByCustomer(customer.getCustomerId()).stream()
            .sorted((a, b) -> b.getCheckInDate().compareTo(a.getCheckInDate()))
            .toList();

        // Summary stats
        long confirmed = reservations.stream()
            .filter(r -> r.getStatus().name().equals("CONFIRMED")).count();
        long completed = reservations.stream()
            .filter(r -> r.getStatus().name().equals("COMPLETED")).count();
        long cancelled = reservations.stream()
            .filter(r -> r.getStatus().name().equals("CANCELLED")).count();
        double totalSpent = reservations.stream()
            .filter(r -> !r.getStatus().name().equals("CANCELLED"))
            .mapToDouble(r -> {
                long n = ChronoUnit.DAYS.between(
                    r.getCheckInDate(), r.getCheckOutDate());
                return n * r.getRoom().getPricePerNight();
            }).sum();
        long totalNights = reservations.stream()
            .filter(r -> !r.getStatus().name().equals("CANCELLED"))
            .mapToLong(r -> ChronoUnit.DAYS.between(
                r.getCheckInDate(), r.getCheckOutDate()))
            .sum();

        HBox stats = new HBox(10);
        stats.getChildren().addAll(
            summaryCard("Total Bookings", String.valueOf(reservations.size()), "#89b4fa"),
            summaryCard("Active",         String.valueOf(confirmed),           "#a6e3a1"),
            summaryCard("Completed",      String.valueOf(completed),           "#cba6f7"),
            summaryCard("Cancelled",      String.valueOf(cancelled),           "#f38ba8"),
            summaryCard("Nights Stayed",  String.valueOf(totalNights),         "#89dceb"),
            summaryCard("Total Spent",    String.format("$%.0f", totalSpent),  "#94e2d5")
        );

        Label histTitle = new Label("Reservation History");
        histTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        histTitle.setStyle("-fx-text-fill:#1e1e2e;");

        if (reservations.isEmpty()) {
            Label empty = new Label("This customer has no reservations yet.");
            empty.setStyle("-fx-text-fill:#6c7086;");
            content.getChildren().addAll(header, stats, histTitle, empty);
        } else {
            TableView<Reservation> table = buildHistoryTable(reservations);
            content.getChildren().addAll(header, stats, histTitle, table);
        }

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        scroll.setPrefHeight(520);

        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().setPrefWidth(660);
        dialog.showAndWait();
    }

    @SuppressWarnings("unchecked")
    private TableView<Reservation> buildHistoryTable(List<Reservation> reservations) {
        TableView<Reservation> t = new TableView<>(
            FXCollections.observableArrayList(reservations));
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        t.setStyle("-fx-background-color:white;-fx-background-radius:8;");
        t.setPrefHeight(220);

        TableColumn<Reservation, String> colRoom = new TableColumn<>("Room");
        colRoom.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getRoom().getRoomNumber()));
        colRoom.setMaxWidth(70);

        TableColumn<Reservation, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getRoom().getType().name()));
        colType.setMaxWidth(80);

        TableColumn<Reservation, String> colIn = new TableColumn<>("Check-in");
        colIn.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getCheckInDate().format(FMT)));

        TableColumn<Reservation, String> colOut = new TableColumn<>("Check-out");
        colOut.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getCheckOutDate().format(FMT)));

        TableColumn<Reservation, String> colNights = new TableColumn<>("Nights");
        colNights.setCellValueFactory(d -> {
            long n = ChronoUnit.DAYS.between(
                d.getValue().getCheckInDate(), d.getValue().getCheckOutDate());
            return new SimpleStringProperty(String.valueOf(n));
        });
        colNights.setMaxWidth(60);

        TableColumn<Reservation, String> colTotal = new TableColumn<>("Cost");
        colTotal.setCellValueFactory(d -> {
            long n = ChronoUnit.DAYS.between(
                d.getValue().getCheckInDate(), d.getValue().getCheckOutDate());
            return new SimpleStringProperty(
                String.format("$%.2f", n * d.getValue().getRoom().getPricePerNight()));
        });
        colTotal.setMaxWidth(90);

        TableColumn<Reservation, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getStatus().name()));
        colStatus.setMaxWidth(105);
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String color = switch (item) {
                    case "CONFIRMED" -> "#a6e3a1";
                    case "CANCELLED" -> "#f38ba8";
                    case "COMPLETED" -> "#89b4fa";
                    default          -> "#cdd6f4";
                };
                setStyle("-fx-background-color:" + color +
                         ";-fx-background-radius:6;-fx-alignment:center;");
            }
        });

        t.getColumns().addAll(colRoom, colType, colIn, colOut, colNights, colTotal, colStatus);
        return t;
    }

    private VBox summaryCard(String label, String value, String color) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setAlignment(Pos.CENTER);
        card.setStyle(
            "-fx-background-color:white;" +
            "-fx-background-radius:8;" +
            "-fx-border-color:" + color + ";" +
            "-fx-border-radius:8;" +
            "-fx-border-width:2;"
        );
        Label val = new Label(value);
        val.setFont(Font.font("System", FontWeight.BOLD, 18));
        val.setStyle("-fx-text-fill:#1e1e2e;");
        Label lbl = new Label(label);
        lbl.setFont(Font.font("System", 10));
        lbl.setStyle("-fx-text-fill:#6c7086;");
        card.getChildren().addAll(val, lbl);
        return card;
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
                    new Alert(Alert.AlertType.ERROR, ex.getMessage(),
                        ButtonType.OK).showAndWait();
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(c ->
            customerList.setAll(Main.customerRepository.findAll()));
    }

    private Button styledButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color:" + color + ";" +
            "-fx-background-radius:6;-fx-cursor:hand;" +
            "-fx-font-size:12;-fx-padding:5 12 5 12;"
        );
        return btn;
    }
}
