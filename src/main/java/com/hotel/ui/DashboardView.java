package com.hotel.ui;

import com.hotel.Main;
import com.hotel.domain.Reservation;
import com.hotel.domain.Room;
import com.hotel.enums.ReservationStatus;
import com.hotel.enums.RoomStatus;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class DashboardView {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yy");

    public VBox build() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f8f8f2;");

        HBox topBar = new HBox();
        topBar.setPadding(new Insets(16, 20, 16, 20));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color:#ffffff;-fx-border-color:#e0e0e0;-fx-border-width:0 0 1 0;");
        Label pageTitle = new Label("Dashboard");
        pageTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        pageTitle.setStyle("-fx-text-fill:#1e1e2e;");
        topBar.getChildren().add(pageTitle);

        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        VBox.setVgrow(content, Priority.ALWAYS);
        content.getChildren().addAll(buildStatCards(), buildRecentReservations());

        root.getChildren().addAll(topBar, content);
        return root;
    }

    private HBox buildStatCards() {
        List<Room> allRooms  = Main.roomService.getAllRooms();
        LocalDate  today     = LocalDate.now();
        List<Reservation> allRes = Main.reservationService.getAllReservations();

        // Rooms physically available (not under maintenance)
        long available   = allRooms.stream()
            .filter(r -> r.getStatus() == RoomStatus.AVAILABLE).count();

        // Maintenance: set manually from Rooms tab
        long maintenance = allRooms.stream()
            .filter(r -> r.getStatus() == RoomStatus.MAINTENANCE).count();

        // Active bookings TODAY (confirmed reservations whose range includes today)
        long activeToday = allRes.stream()
            .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
            .filter(r -> !r.getCheckInDate().isAfter(today) &&
                          r.getCheckOutDate().isAfter(today))
            .count();

        // Check-ins today
        long checkInsToday = allRes.stream()
            .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
            .filter(r -> r.getCheckInDate().equals(today))
            .count();

        // Check-outs today
        long checkOutsToday = allRes.stream()
            .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED ||
                         r.getStatus() == ReservationStatus.COMPLETED)
            .filter(r -> r.getCheckOutDate().equals(today))
            .count();

        long customers = Main.customerRepository.findAll().size();

        double revenue = allRes.stream()
            .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED ||
                         r.getStatus() == ReservationStatus.COMPLETED)
            .mapToDouble(r -> {
                long n = ChronoUnit.DAYS.between(r.getCheckInDate(), r.getCheckOutDate());
                return n * r.getRoom().getPricePerNight();
            }).sum();

        HBox cards = new HBox(10);
        cards.getChildren().addAll(
            statCard("Total Rooms",      String.valueOf(allRooms.size()), "#89b4fa", "#e8f0fe"),
            statCard("Available",        String.valueOf(available),       "#a6e3a1", "#e8f5e9"),
            statCard("Occupied Today",   String.valueOf(activeToday),     "#f9e2af", "#fff8e1"),
            statCard("Maintenance",      String.valueOf(maintenance),     "#f38ba8", "#fce4ec"),
            statCard("Check-ins Today",  String.valueOf(checkInsToday),   "#cba6f7", "#f3e5f5"),
            statCard("Check-outs Today", String.valueOf(checkOutsToday),  "#89dceb", "#e0f7f5"),
            statCard("Customers",        String.valueOf(customers),       "#b4befe", "#eef0ff"),
            statCard("Revenue",          String.format("$%.0f", revenue), "#94e2d5", "#e0f7f5")
        );
        return cards;
    }

    private VBox statCard(String label, String value, String accent, String bg) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14));
        card.setPrefWidth(135);
        card.setStyle(
            "-fx-background-color:" + bg + ";" +
            "-fx-background-radius:10;" +
            "-fx-border-color:" + accent + ";" +
            "-fx-border-width:0 0 0 4;" +
            "-fx-border-radius:10;"
        );
        Label lbl = new Label(label);
        lbl.setFont(Font.font("System", 11));
        lbl.setStyle("-fx-text-fill:#6c7086;");
        Label val = new Label(value);
        val.setFont(Font.font("System", FontWeight.BOLD, 24));
        val.setStyle("-fx-text-fill:#1e1e2e;");
        card.getChildren().addAll(lbl, val);
        return card;
    }

    private VBox buildRecentReservations() {
        VBox box = new VBox(10);
        VBox.setVgrow(box, Priority.ALWAYS);

        Label title = new Label("Recent Reservations");
        title.setFont(Font.font("System", FontWeight.BOLD, 14));
        title.setStyle("-fx-text-fill:#1e1e2e;");

        TableView<Reservation> table = buildReservationTable();
        VBox.setVgrow(table, Priority.ALWAYS);
        box.getChildren().addAll(title, table);
        return box;
    }

    @SuppressWarnings("unchecked")
    private TableView<Reservation> buildReservationTable() {
        TableView<Reservation> table = new TableView<>();
        table.setStyle("-fx-background-color:white;-fx-background-radius:10;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Reservation, String> colGuest = new TableColumn<>("Guest");
        colGuest.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(
                d.getValue().getCustomer().getFullName()));

        TableColumn<Reservation, String> colRoom = new TableColumn<>("Room");
        colRoom.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(
                d.getValue().getRoom().getRoomNumber()));
        colRoom.setMaxWidth(70);

        TableColumn<Reservation, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(
                d.getValue().getRoom().getType().name()));
        colType.setMaxWidth(80);

        TableColumn<Reservation, String> colIn = new TableColumn<>("Check-in");
        colIn.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(
                d.getValue().getCheckInDate().format(FMT)));

        TableColumn<Reservation, String> colOut = new TableColumn<>("Check-out");
        colOut.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(
                d.getValue().getCheckOutDate().format(FMT)));

        TableColumn<Reservation, String> colNights = new TableColumn<>("Nights");
        colNights.setCellValueFactory(d -> {
            long n = ChronoUnit.DAYS.between(
                d.getValue().getCheckInDate(), d.getValue().getCheckOutDate());
            return new javafx.beans.property.SimpleStringProperty(String.valueOf(n));
        });
        colNights.setMaxWidth(60);

        TableColumn<Reservation, String> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(d -> {
            long n = ChronoUnit.DAYS.between(
                d.getValue().getCheckInDate(), d.getValue().getCheckOutDate());
            double total = n * d.getValue().getRoom().getPricePerNight();
            return new javafx.beans.property.SimpleStringProperty(
                String.format("$%.2f", total));
        });
        colTotal.setMaxWidth(90);

        TableColumn<Reservation, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(
                d.getValue().getStatus().name()));
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

        table.getColumns().addAll(
            colGuest, colRoom, colType, colIn, colOut, colNights, colTotal, colStatus);
        table.getItems().addAll(Main.reservationService.getAllReservations());
        return table;
    }
}
