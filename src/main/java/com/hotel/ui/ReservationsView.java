package com.hotel.ui;

import com.hotel.Main;
import com.hotel.domain.Customer;
import com.hotel.domain.Reservation;
import com.hotel.domain.Room;
import com.hotel.enums.ReservationStatus;
import com.hotel.enums.RoomStatus;
import com.hotel.exception.ReservationConflictException;
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

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReservationsView {

    private ObservableList<Reservation> resList;
    private TableView<Reservation> table;

    public VBox build() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f8f8f2;");
        root.getChildren().addAll(buildTopBar(), buildFilters(), buildContent());
        return root;
    }

    // ── Top bar ────────────────────────────────────────────────────────────────
    private HBox buildTopBar() {
        HBox bar = new HBox();
        bar.setPadding(new Insets(16, 20, 16, 20));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color:#ffffff;-fx-border-color:#e0e0e0;-fx-border-width:0 0 1 0;");

        Label title = new Label("Reservations");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        title.setTextFill(Color.web("#1e1e2e"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnNew = styledButton("+ New Reservation", "#89b4fa");
        btnNew.setOnAction(e -> showNewReservationDialog());

        bar.getChildren().addAll(title, spacer, btnNew);
        return bar;
    }

    // ── Filters bar ────────────────────────────────────────────────────────────
    private HBox buildFilters() {
        HBox bar = new HBox(10);
        bar.setPadding(new Insets(10, 20, 10, 20));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color:#f0f0f0;");

        // Date range filter
        DatePicker dpFrom = new DatePicker();
        dpFrom.setPromptText("From");
        dpFrom.setPrefWidth(135);

        DatePicker dpTo = new DatePicker();
        dpTo.setPromptText("To");
        dpTo.setPrefWidth(135);

        // ── FIX 3: Search by customer ──────────────────────────────────────────
        ComboBox<Customer> cbCustomer = new ComboBox<>(
            FXCollections.observableArrayList(Main.customerRepository.findAll()));
        cbCustomer.setPromptText("All customers");
        cbCustomer.setPrefWidth(200);
        cbCustomer.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Customer c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null : c.getFullName());
            }
        });
        cbCustomer.setButtonCell(cbCustomer.getCellFactory().call(null));

        Button btnSearch = styledButton("Search", "#cba6f7");
        Button btnClear  = styledButton("Clear",  "#6c7086");
        btnClear.setStyle(btnClear.getStyle() + "-fx-text-fill:white;");

        btnSearch.setOnAction(e -> {
            List<Reservation> result = Main.reservationService.getAllReservations();

            // Filter by date range if set
            if (dpFrom.getValue() != null && dpTo.getValue() != null) {
                result = Main.reservationService
                    .getReservationsByDateRange(dpFrom.getValue(), dpTo.getValue());
            }

            // Filter by customer if selected
            if (cbCustomer.getValue() != null) {
                final List<Reservation> fromDate = result;
                result = Main.reservationService
                    .getReservationsByCustomer(cbCustomer.getValue().getCustomerId())
                    .stream()
                    .filter(fromDate::contains)
                    .toList();
            }

            resList.setAll(result);
        });

        btnClear.setOnAction(e -> {
            dpFrom.setValue(null);
            dpTo.setValue(null);
            cbCustomer.setValue(null);
            resList.setAll(Main.reservationService.getAllReservations());
        });

        bar.getChildren().addAll(
            new Label("From:"), dpFrom,
            new Label("To:"),   dpTo,
            new Label("Guest:"), cbCustomer,
            btnSearch, btnClear
        );
        return bar;
    }

    // ── Table content ──────────────────────────────────────────────────────────
    private VBox buildContent() {
        VBox content = new VBox(14);
        content.setPadding(new Insets(20));
        VBox.setVgrow(content, Priority.ALWAYS);

        resList = FXCollections.observableArrayList(Main.reservationService.getAllReservations());
        table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);
        content.getChildren().add(table);
        return content;
    }

    @SuppressWarnings("unchecked")
    private TableView<Reservation> buildTable() {
        TableView<Reservation> t = new TableView<>(resList);
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        t.setStyle("-fx-background-color:white;-fx-background-radius:10;");

        TableColumn<Reservation, String> colGuest = new TableColumn<>("Guest");
        colGuest.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getCustomer().getFullName()));

        TableColumn<Reservation, String> colRoom = new TableColumn<>("Room");
        colRoom.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getRoom().getRoomNumber()));
        colRoom.setMaxWidth(75);

        TableColumn<Reservation, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getRoom().getType().name()));
        colType.setMaxWidth(85);

        TableColumn<Reservation, String> colIn = new TableColumn<>("Check-in");
        colIn.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getCheckInDate().toString()));

        TableColumn<Reservation, String> colOut = new TableColumn<>("Check-out");
        colOut.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getCheckOutDate().toString()));

        TableColumn<Reservation, String> colNights = new TableColumn<>("Nights");
        colNights.setCellValueFactory(d -> {
            long nights = ChronoUnit.DAYS.between(
                d.getValue().getCheckInDate(), d.getValue().getCheckOutDate());
            return new SimpleStringProperty(String.valueOf(nights));
        });
        colNights.setMaxWidth(65);

        // ── FIX 2: Total cost column ───────────────────────────────────────────
        TableColumn<Reservation, String> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(d -> {
            long nights = ChronoUnit.DAYS.between(
                d.getValue().getCheckInDate(), d.getValue().getCheckOutDate());
            double total = nights * d.getValue().getRoom().getPricePerNight();
            return new SimpleStringProperty(String.format("$%.2f", total));
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

        TableColumn<Reservation, Void> colActions = new TableColumn<>("Actions");
        colActions.setMaxWidth(185);
        colActions.setCellFactory(col -> new TableCell<>() {
            final Button btnCancel   = styledButton("Cancel",   "#f38ba8");
            final Button btnComplete = styledButton("Complete", "#89b4fa");
            final HBox   box         = new HBox(6, btnCancel, btnComplete);
            {
                box.setAlignment(Pos.CENTER);

                btnCancel.setOnAction(e -> {
                    Reservation r = getTableView().getItems().get(getIndex());
                    if (r.getStatus() != ReservationStatus.CONFIRMED) {
                        showError("Only CONFIRMED reservations can be cancelled.");
                        return;
                    }
                    Main.reservationService.cancelReservation(r.getReservationId());
                    // ── FIX 4: restore room to AVAILABLE on cancel ─────────────
                    Main.roomService.updateRoomStatus(r.getRoom().getRoomId(), RoomStatus.AVAILABLE);
                    resList.setAll(Main.reservationService.getAllReservations());
                });

                btnComplete.setOnAction(e -> {
                    Reservation r = getTableView().getItems().get(getIndex());
                    if (r.getStatus() != ReservationStatus.CONFIRMED) {
                        showError("Only CONFIRMED reservations can be completed.");
                        return;
                    }
                    Main.reservationService.completeReservation(r.getReservationId());
                    // ── FIX 4: restore room to AVAILABLE on complete ───────────
                    Main.roomService.updateRoomStatus(r.getRoom().getRoomId(), RoomStatus.AVAILABLE);
                    resList.setAll(Main.reservationService.getAllReservations());
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        t.getColumns().addAll(colGuest, colRoom, colType, colIn, colOut,
                              colNights, colTotal, colStatus, colActions);
        return t;
    }

    // ── New reservation dialog ─────────────────────────────────────────────────
    private void showNewReservationDialog() {
        List<Customer> customers = Main.customerRepository.findAll();
        if (customers.isEmpty()) {
            showError("No customers found. Please add a customer first.");
            return;
        }

        Dialog<Reservation> dialog = new Dialog<>();
        dialog.setTitle("New Reservation");
        dialog.setHeaderText("Fill in the reservation details");

        ButtonType bookBtn = new ButtonType("Book", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(bookBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(14);
        grid.setPadding(new Insets(20));

        // Customer dropdown
        ComboBox<Customer> cbCustomer = new ComboBox<>(
            FXCollections.observableArrayList(customers));
        cbCustomer.setPromptText("Select customer...");
        cbCustomer.setPrefWidth(300);
        cbCustomer.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Customer c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null : c.getFullName() + " (" + c.getEmail() + ")");
            }
        });
        cbCustomer.setButtonCell(cbCustomer.getCellFactory().call(null));

        // Date pickers
        DatePicker dpIn  = new DatePicker(LocalDate.now());
        DatePicker dpOut = new DatePicker(LocalDate.now().plusDays(3));
        dpIn.setPrefWidth(300);
        dpOut.setPrefWidth(300);

        // ── FIX 2: Cost preview label ──────────────────────────────────────────
        Label lblCost = new Label("Total cost: —");
        lblCost.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblCost.setTextFill(Color.web("#1e1e2e"));

        // ── FIX 1: Room dropdown — only available rooms for selected dates ─────
        ComboBox<Room> cbRoom = new ComboBox<>();
        cbRoom.setPromptText("Select dates first...");
        cbRoom.setPrefWidth(300);
        cbRoom.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Room r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? null :
                    "Room " + r.getRoomNumber() + " | " + r.getType() +
                    " | $" + String.format("%.2f", r.getPricePerNight()) + "/night");
            }
        });
        cbRoom.setButtonCell(cbRoom.getCellFactory().call(null));

        // Refresh available rooms and cost whenever dates change
        Runnable refreshRooms = () -> {
            LocalDate in  = dpIn.getValue();
            LocalDate out = dpOut.getValue();
            if (in != null && out != null && out.isAfter(in)) {
                List<Room> available = Main.roomService.getAvailableRoomsForDateRange(in, out);
                cbRoom.setItems(FXCollections.observableArrayList(available));
                cbRoom.setPromptText(available.isEmpty() ? "No rooms available" : "Select room...");
                cbRoom.setValue(null);
                lblCost.setText("Total cost: —");
            }
        };

        dpIn.valueProperty().addListener((obs, o, n) -> refreshRooms.run());
        dpOut.valueProperty().addListener((obs, o, n) -> refreshRooms.run());

        // Update cost preview when room is selected
        cbRoom.valueProperty().addListener((obs, o, room) -> {
            if (room != null && dpIn.getValue() != null && dpOut.getValue() != null) {
                long nights = ChronoUnit.DAYS.between(dpIn.getValue(), dpOut.getValue());
                double total = nights * room.getPricePerNight();
                lblCost.setText(String.format(
                    "Total cost: %d nights x $%.2f = $%.2f", nights, room.getPricePerNight(), total));
                lblCost.setTextFill(Color.web("#40a02b"));
            }
        });

        // Trigger initial load
        refreshRooms.run();

        grid.addRow(0, new Label("Customer:"),  cbCustomer);
        grid.addRow(1, new Label("Check-in:"),  dpIn);
        grid.addRow(2, new Label("Check-out:"), dpOut);
        grid.addRow(3, new Label("Room:"),      cbRoom);
        grid.addRow(4, new Label(""),           lblCost);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(480);

        dialog.setResultConverter(btn -> {
            if (btn == bookBtn) {
                if (cbCustomer.getValue() == null || cbRoom.getValue() == null
                        || dpIn.getValue() == null || dpOut.getValue() == null) {
                    showError("Please fill in all fields.");
                    return null;
                }
                try {
                    Reservation res = Main.reservationService.createReservation(
                        cbCustomer.getValue().getCustomerId(),
                        cbRoom.getValue().getRoomId(),
                        dpIn.getValue(), dpOut.getValue());

                    // ── FIX 4: Mark room as OCCUPIED on booking ────────────────
                    Main.roomService.updateRoomStatus(
                        cbRoom.getValue().getRoomId(), RoomStatus.OCCUPIED);

                    return res;
                } catch (ReservationConflictException ex) {
                    showError("Booking conflict: " + ex.getMessage());
                } catch (Exception ex) {
                    showError("Error: " + ex.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(r -> {
            resList.setAll(Main.reservationService.getAllReservations());
            long nights = ChronoUnit.DAYS.between(r.getCheckInDate(), r.getCheckOutDate());
            double total = nights * r.getRoom().getPricePerNight();
            showInfo(String.format(
                "Reservation confirmed!\n\nGuest: %s\nRoom: %s (%s)\nDates: %s to %s\nNights: %d\nTotal: $%.2f",
                r.getCustomer().getFullName(),
                r.getRoom().getRoomNumber(),
                r.getRoom().getType(),
                r.getCheckInDate(), r.getCheckOutDate(),
                nights, total));
        });
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private Button styledButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:" + color + ";-fx-background-radius:6;" +
                     "-fx-cursor:hand;-fx-font-size:12;-fx-padding:5 12 5 12;");
        return btn;
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }
}
