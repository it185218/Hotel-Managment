package com.hotel.ui;

import com.hotel.Main;
import com.hotel.domain.Reservation;
import com.hotel.domain.Room;
import com.hotel.enums.RoomStatus;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.temporal.ChronoUnit;
import java.util.List;

public class DashboardView {

    public VBox build() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f8f8f2;");

        HBox topBar = new HBox();
        topBar.setPadding(new Insets(16, 20, 16, 20));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color:#ffffff;-fx-border-color:#e0e0e0;-fx-border-width:0 0 1 0;");
        Label pageTitle = new Label("Dashboard");
        pageTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        pageTitle.setTextFill(Color.web("#1e1e2e"));
        topBar.getChildren().add(pageTitle);

        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        VBox.setVgrow(content, Priority.ALWAYS);
        content.getChildren().addAll(buildStatCards(), buildRecentReservations());

        root.getChildren().addAll(topBar, content);
        return root;
    }

    private HBox buildStatCards() {
        List<Room> all       = Main.roomService.getAllRooms();
        long available   = all.stream().filter(r -> r.getStatus() == RoomStatus.AVAILABLE).count();
        long occupied    = all.stream().filter(r -> r.getStatus() == RoomStatus.OCCUPIED).count();
        long maintenance = all.stream().filter(r -> r.getStatus() == RoomStatus.MAINTENANCE).count();
        long customers   = Main.customerRepository.findAll().size();

        // Total revenue from CONFIRMED reservations
        double revenue = Main.reservationService.getAllReservations().stream()
            .filter(r -> r.getStatus().name().equals("CONFIRMED") ||
                         r.getStatus().name().equals("COMPLETED"))
            .mapToDouble(r -> {
                long nights = ChronoUnit.DAYS.between(r.getCheckInDate(), r.getCheckOutDate());
                return nights * r.getRoom().getPricePerNight();
            }).sum();

        HBox cards = new HBox(12);
        cards.getChildren().addAll(
            statCard("Total Rooms",  String.valueOf(all.size()),  "#89b4fa", "#e8f0fe"),
            statCard("Available",    String.valueOf(available),   "#a6e3a1", "#e8f5e9"),
            statCard("Occupied",     String.valueOf(occupied),    "#f9e2af", "#fff8e1"),
            statCard("Maintenance",  String.valueOf(maintenance), "#f38ba8", "#fce4ec"),
            statCard("Customers",    String.valueOf(customers),   "#cba6f7", "#f3e5f5"),
            statCard("Revenue",      String.format("$%.0f", revenue), "#94e2d5", "#e0f7f5")
        );
        return cards;
    }

    private VBox statCard(String label, String value, String accent, String bg) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(16));
        card.setPrefWidth(160);
        card.setStyle("-fx-background-color:" + bg + ";-fx-background-radius:10;" +
                      "-fx-border-color:" + accent + ";-fx-border-width:0 0 0 4;-fx-border-radius:10;");
        Label lbl = new Label(label);
        lbl.setFont(Font.font("System", 12));
        lbl.setTextFill(Color.web("#6c7086"));
        Label val = new Label(value);
        val.setFont(Font.font("System", FontWeight.BOLD, 24));
        val.setTextFill(Color.web("#1e1e2e"));
        card.getChildren().addAll(lbl, val);
        return card;
    }

    private VBox buildRecentReservations() {
        VBox box = new VBox(10);
        VBox.setVgrow(box, Priority.ALWAYS);

        Label title = new Label("Recent Reservations");
        title.setFont(Font.font("System", FontWeight.BOLD, 14));
        title.setTextFill(Color.web("#1e1e2e"));

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
            new javafx.beans.property.SimpleStringProperty(d.getValue().getCustomer().getFullName()));

        TableColumn<Reservation, String> colRoom = new TableColumn<>("Room");
        colRoom.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(d.getValue().getRoom().getRoomNumber()));
        colRoom.setMaxWidth(75);

        TableColumn<Reservation, String> colIn = new TableColumn<>("Check-in");
        colIn.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(d.getValue().getCheckInDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy"))));

        TableColumn<Reservation, String> colOut = new TableColumn<>("Check-out");
        colOut.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(d.getValue().getCheckOutDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy"))));

        TableColumn<Reservation, String> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(d -> {
            long nights = ChronoUnit.DAYS.between(
                d.getValue().getCheckInDate(), d.getValue().getCheckOutDate());
            double total = nights * d.getValue().getRoom().getPricePerNight();
            return new javafx.beans.property.SimpleStringProperty(String.format("$%.2f", total));
        });
        colTotal.setMaxWidth(90);

        TableColumn<Reservation, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(d.getValue().getStatus().name()));
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

        table.getColumns().addAll(colGuest, colRoom, colIn, colOut, colTotal, colStatus);
        table.getItems().addAll(Main.reservationService.getAllReservations());
        return table;
    }
}
