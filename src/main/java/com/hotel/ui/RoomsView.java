package com.hotel.ui;

import com.hotel.Main;
import com.hotel.domain.Reservation;
import com.hotel.domain.Room;
import com.hotel.enums.RoomStatus;
import com.hotel.enums.RoomType;
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

public class RoomsView {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yy");
    private ObservableList<Room> roomList;

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

        Label title = new Label("Rooms");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        title.setStyle("-fx-text-fill:#1e1e2e;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAdd = actionButton("+ Add Room", "#89b4fa");
        btnAdd.setOnAction(e -> showAddRoomDialog());

        bar.getChildren().addAll(title, spacer, btnAdd);
        return bar;
    }

    private VBox buildContent() {
        VBox content = new VBox(14);
        content.setPadding(new Insets(20));
        VBox.setVgrow(content, Priority.ALWAYS);

        roomList = FXCollections.observableArrayList(Main.roomService.getAllRooms());
        TableView<Room> table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);
        content.getChildren().add(table);
        return content;
    }

    @SuppressWarnings("unchecked")
    private TableView<Room> buildTable() {
        TableView<Room> t = new TableView<>(roomList);
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        t.setStyle("-fx-background-color:white;-fx-background-radius:10;");

        TableColumn<Room, String> colNum = new TableColumn<>("Room #");
        colNum.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getRoomNumber()));
        colNum.setMaxWidth(90);

        TableColumn<Room, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getType().name()));

        TableColumn<Room, String> colPrice = new TableColumn<>("Price / Night");
        colPrice.setCellValueFactory(d ->
            new SimpleStringProperty(String.format("$%.2f", d.getValue().getPricePerNight())));

        TableColumn<Room, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getStatus().name()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String color = switch (item) {
                    case "AVAILABLE"   -> "#a6e3a1";
                    case "OCCUPIED"    -> "#f9e2af";
                    case "MAINTENANCE" -> "#f38ba8";
                    default            -> "#cdd6f4";
                };
                setStyle("-fx-background-color:" + color +
                         ";-fx-background-radius:6;-fx-alignment:center;");
            }
        });

        // Active reservations count
        TableColumn<Room, String> colBookings = new TableColumn<>("Active Bookings");
        colBookings.setCellValueFactory(d -> {
            long count = Main.reservationService.getAllReservations().stream()
                .filter(r -> r.getRoom().getRoomId().equals(d.getValue().getRoomId()))
                .filter(r -> r.getStatus().name().equals("CONFIRMED"))
                .count();
            return new SimpleStringProperty(String.valueOf(count));
        });
        colBookings.setMaxWidth(120);

        TableColumn<Room, Void> colActions = new TableColumn<>("Actions");
        colActions.setCellFactory(col -> new TableCell<>() {
            final Button btnInfo   = actionButton("Info",   "#89b4fa");
            final Button btnStatus = actionButton("Status", "#cba6f7");
            final Button btnDelete = actionButton("Delete", "#f38ba8");
            final HBox   box       = new HBox(6, btnInfo, btnStatus, btnDelete);
            {
                box.setAlignment(Pos.CENTER);
                btnInfo.setOnAction(e -> {
                    Room r = getTableView().getItems().get(getIndex());
                    showRoomInfoDialog(r);
                });
                btnStatus.setOnAction(e -> {
                    Room r = getTableView().getItems().get(getIndex());
                    showChangeStatusDialog(r);
                });
                btnDelete.setOnAction(e -> {
                    Room r = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Delete room " + r.getRoomNumber() + "?",
                        ButtonType.YES, ButtonType.NO);
                    confirm.showAndWait().ifPresent(btn -> {
                        if (btn == ButtonType.YES) {
                            Main.roomRepository.delete(r.getRoomId());
                            roomList.setAll(Main.roomService.getAllRooms());
                        }
                    });
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        t.getColumns().addAll(colNum, colType, colPrice, colStatus, colBookings, colActions);
        return t;
    }

    // ── Room Info Dialog ───────────────────────────────────────────────────────
    private void showRoomInfoDialog(Room room) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Room " + room.getRoomNumber() + " - Information");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(16);
        content.setPadding(new Insets(20));
        content.setPrefWidth(580);

        // Room details header
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16));
        header.setStyle(
            "-fx-background-color:#e8f0fe;-fx-background-radius:10;" +
            "-fx-border-color:#89b4fa;-fx-border-radius:10;-fx-border-width:0 0 0 4;"
        );

        VBox roomDetails = new VBox(6);
        Label roomNum = new Label("Room " + room.getRoomNumber());
        roomNum.setFont(Font.font("System", FontWeight.BOLD, 18));
        roomNum.setStyle("-fx-text-fill:#1e1e2e;");

        Label roomInfo = new Label(
            room.getType().name() + "  |  $" +
            String.format("%.2f", room.getPricePerNight()) + " per night  |  " +
            room.getStatus().name());
        roomInfo.setFont(Font.font("System", 13));
        roomInfo.setStyle("-fx-text-fill:#6c7086;");

        roomDetails.getChildren().addAll(roomNum, roomInfo);
        header.getChildren().add(roomDetails);

        // Reservations for this room
        Label resTitle = new Label("Reservation History");
        resTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        resTitle.setStyle("-fx-text-fill:#1e1e2e;");

        List<Reservation> reservations = Main.reservationService
            .getAllReservations().stream()
            .filter(r -> r.getRoom().getRoomId().equals(room.getRoomId()))
            .sorted((a, b) -> b.getCheckInDate().compareTo(a.getCheckInDate()))
            .toList();

        if (reservations.isEmpty()) {
            Label empty = new Label("No reservations found for this room.");
            empty.setStyle("-fx-text-fill:#6c7086;");
            content.getChildren().addAll(header, resTitle, empty);
        } else {
            TableView<Reservation> table = buildReservationTable(reservations);
            content.getChildren().addAll(header, resTitle, table);
        }

        // Summary stats
        long confirmed = reservations.stream()
            .filter(r -> r.getStatus().name().equals("CONFIRMED")).count();
        long completed = reservations.stream()
            .filter(r -> r.getStatus().name().equals("COMPLETED")).count();
        double totalRevenue = reservations.stream()
            .filter(r -> !r.getStatus().name().equals("CANCELLED"))
            .mapToDouble(r -> {
                long n = ChronoUnit.DAYS.between(r.getCheckInDate(), r.getCheckOutDate());
                return n * r.getRoom().getPricePerNight();
            }).sum();

        HBox stats = new HBox(14);
        stats.getChildren().addAll(
            summaryCard("Total Bookings", String.valueOf(reservations.size()), "#89b4fa"),
            summaryCard("Active",         String.valueOf(confirmed),           "#a6e3a1"),
            summaryCard("Completed",      String.valueOf(completed),           "#cba6f7"),
            summaryCard("Revenue",        String.format("$%.0f", totalRevenue),"#94e2d5")
        );

        content.getChildren().add(0, stats);
        content.getChildren().add(1, header);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        scroll.setPrefHeight(500);

        dialog.getDialogPane().setContent(scroll);
        dialog.showAndWait();
    }

    @SuppressWarnings("unchecked")
    private TableView<Reservation> buildReservationTable(List<Reservation> reservations) {
        TableView<Reservation> t = new TableView<>(
            FXCollections.observableArrayList(reservations));
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        t.setStyle("-fx-background-color:white;-fx-background-radius:8;");
        t.setPrefHeight(200);

        TableColumn<Reservation, String> colGuest = new TableColumn<>("Guest");
        colGuest.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getCustomer().getFullName()));

        TableColumn<Reservation, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getCustomer().getEmail()));

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
        colNights.setMaxWidth(65);

        TableColumn<Reservation, String> colTotal = new TableColumn<>("Total");
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
        colStatus.setMaxWidth(100);
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

        t.getColumns().addAll(colGuest, colEmail, colIn, colOut, colNights, colTotal, colStatus);
        return t;
    }

    private VBox summaryCard(String label, String value, String color) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(10, 16, 10, 16));
        card.setAlignment(Pos.CENTER);
        card.setStyle(
            "-fx-background-color:white;" +
            "-fx-background-radius:8;" +
            "-fx-border-color:" + color + ";" +
            "-fx-border-radius:8;" +
            "-fx-border-width:2;"
        );
        Label val = new Label(value);
        val.setFont(Font.font("System", FontWeight.BOLD, 20));
        val.setStyle("-fx-text-fill:#1e1e2e;");
        Label lbl = new Label(label);
        lbl.setFont(Font.font("System", 11));
        lbl.setStyle("-fx-text-fill:#6c7086;");
        card.getChildren().addAll(val, lbl);
        return card;
    }

    // ── Add / Status dialogs ───────────────────────────────────────────────────
    private void showAddRoomDialog() {
        Dialog<Room> dialog = new Dialog<>();
        dialog.setTitle("Add New Room");
        dialog.setHeaderText("Enter room details");

        ButtonType addBtn = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField tfNumber = new TextField();
        tfNumber.setPromptText("e.g. 105");
        ComboBox<RoomType> cbType = new ComboBox<>();
        cbType.getItems().addAll(RoomType.values());
        cbType.setValue(RoomType.SINGLE);
        TextField tfPrice = new TextField("120.00");

        grid.addRow(0, new Label("Room Number:"), tfNumber);
        grid.addRow(1, new Label("Type:"),        cbType);
        grid.addRow(2, new Label("Price/Night:"), tfPrice);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == addBtn) {
                try {
                    return Main.roomService.createRoom(
                        tfNumber.getText().trim(),
                        cbType.getValue(),
                        Double.parseDouble(tfPrice.getText().trim()));
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK).showAndWait();
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(r ->
            roomList.setAll(Main.roomService.getAllRooms()));
    }

    private void showChangeStatusDialog(Room room) {
        Dialog<RoomStatus> dialog = new Dialog<>();
        dialog.setTitle("Change Status");
        dialog.setHeaderText("Room " + room.getRoomNumber() +
                             " - current: " + room.getStatus());

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        ComboBox<RoomStatus> cb = new ComboBox<>();
        cb.getItems().addAll(RoomStatus.values());
        cb.setValue(room.getStatus());

        VBox box = new VBox(10, new Label("New Status:"), cb);
        box.setPadding(new Insets(20));
        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(btn -> btn == saveBtn ? cb.getValue() : null);
        dialog.showAndWait().ifPresent(status -> {
            Main.roomService.updateRoomStatus(room.getRoomId(), status);
            roomList.setAll(Main.roomService.getAllRooms());
        });
    }

    private Button actionButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color:" + color + ";" +
            "-fx-background-radius:6;-fx-cursor:hand;" +
            "-fx-font-size:12;-fx-padding:5 12 5 12;"
        );
        return btn;
    }
}
