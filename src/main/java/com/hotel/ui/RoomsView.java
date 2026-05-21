package com.hotel.ui;

import com.hotel.Main;
import com.hotel.domain.Floor;
import com.hotel.domain.Reservation;
import com.hotel.domain.Room;
import com.hotel.enums.RoomStatus;
import com.hotel.enums.RoomType;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

public class RoomsView {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yy");

    private TabPane tabPane;

    public VBox build() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:#f8f8f2;");
        root.getChildren().addAll(buildTopBar(), buildBody());
        return root;
    }

    // ── Top bar ────────────────────────────────────────────────────────────────
    private HBox buildTopBar() {
        HBox bar = new HBox(10);
        bar.setPadding(new Insets(12, 20, 12, 20));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color:#ffffff;-fx-border-color:#e0e0e0;-fx-border-width:0 0 1 0;");

        Label title = new Label("Rooms");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        title.setStyle("-fx-text-fill:#1e1e2e;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAddFloor = actionButton("+ Add Floor", "#cba6f7");
        Button btnAddRoom  = actionButton("+ Add Room",  "#89b4fa");
        btnAddFloor.setOnAction(e -> showAddFloorDialog());
        btnAddRoom.setOnAction(e  -> showAddRoomDialog());

        bar.getChildren().addAll(title, spacer, btnAddFloor, btnAddRoom);
        return bar;
    }

    // ── Body with tabs ─────────────────────────────────────────────────────────
    private VBox buildBody() {
        VBox body = new VBox(0);
        VBox.setVgrow(body, Priority.ALWAYS);
        body.setPadding(new Insets(16, 20, 20, 20));

        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color:#f8f8f2;");
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        refreshTabs();

        body.getChildren().add(tabPane);
        return body;
    }

    // ── Refresh all tabs ───────────────────────────────────────────────────────
    private void refreshTabs() {
        tabPane.getTabs().clear();

        // "All Rooms" tab always first
        Tab allTab = new Tab("All Rooms");
        allTab.setContent(buildRoomTable(Main.roomService.getAllRooms()));
        tabPane.getTabs().add(allTab);

        // One tab per floor
        List<Floor> floors = Main.floorRepository.findAll();
        for (Floor floor : floors) {
            Tab tab = new Tab(floor.getDisplayName());
            List<Room> floorRooms = Main.roomRepository.findByFloorId(floor.getFloorId());
            tab.setContent(buildFloorTab(floor, floorRooms));
            tabPane.getTabs().add(tab);
        }

        // "Unassigned" tab for rooms without a floor
        List<Room> unassigned = Main.roomService.getAllRooms().stream()
            .filter(r -> r.getFloor() == null)
            .toList();
        if (!unassigned.isEmpty()) {
            Tab unTab = new Tab("Unassigned");
            unTab.setContent(buildRoomTable(unassigned));
            tabPane.getTabs().add(unTab);
        }
    }

    // ── Floor tab content ──────────────────────────────────────────────────────
    private VBox buildFloorTab(Floor floor, List<Room> rooms) {
        VBox box = new VBox(12);
        box.setPadding(new Insets(14));

        // Floor header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 16, 10, 16));
        header.setStyle(
            "-fx-background-color:#f0e6ff;-fx-background-radius:8;" +
            "-fx-border-color:#cba6f7;-fx-border-radius:8;-fx-border-width:0 0 0 4;");

        Label floorLabel = new Label("Floor " + floor.getFloorNumber());
        floorLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
        floorLabel.setStyle("-fx-text-fill:#1e1e2e;");

        Label descLabel = new Label(floor.getDescription().isBlank() ? "" : " - " + floor.getDescription());
        descLabel.setStyle("-fx-text-fill:#6c7086;");
        descLabel.setFont(Font.font("System", 13));

        Label countLabel = new Label(rooms.size() + " room" + (rooms.size() != 1 ? "s" : ""));
        countLabel.setStyle("-fx-text-fill:#cba6f7;-fx-font-size:12;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnDeleteFloor = actionButton("Delete Floor", "#f38ba8");
        btnDeleteFloor.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete Floor " + floor.getFloorNumber() + "?\nRooms will become unassigned.",
                ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(b -> {
                if (b == ButtonType.YES) {
                    Main.floorRepository.delete(floor.getFloorId());
                    refreshTabs();
                }
            });
        });

        header.getChildren().addAll(floorLabel, descLabel, spacer, countLabel, btnDeleteFloor);

        VBox tableBox = buildRoomTable(rooms);
        VBox.setVgrow(tableBox, Priority.ALWAYS);

        box.getChildren().addAll(header, tableBox);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    // ── Room table (shared) ────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private VBox buildRoomTable(List<Room> rooms) {
        ObservableList<Room> roomList = FXCollections.observableArrayList(rooms);

        TableView<Room> t = new TableView<>(roomList);
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        t.setStyle("-fx-background-color:white;-fx-background-radius:10;");
        VBox.setVgrow(t, Priority.ALWAYS);

        // Double-click opens info
        t.setRowFactory(tv -> {
            TableRow<Room> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty())
                    showRoomInfoDialog(row.getItem());
            });
            return row;
        });

        TableColumn<Room, String> colNum = new TableColumn<>("Room #");
        colNum.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRoomNumber()));
        colNum.setMaxWidth(80);

        TableColumn<Room, String> colFloor = new TableColumn<>("Floor");
        colFloor.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getFloor() != null ? "Floor " + d.getValue().getFloor().getFloorNumber() : "-"));
        colFloor.setMaxWidth(80);

        TableColumn<Room, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType().name()));
        colType.setMaxWidth(90);

        TableColumn<Room, String> colPrice = new TableColumn<>("Price/Night");
        colPrice.setCellValueFactory(d ->
            new SimpleStringProperty(String.format("$%.2f", d.getValue().getPricePerNight())));
        colPrice.setMaxWidth(100);

        TableColumn<Room, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
        colStatus.setMaxWidth(110);
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
                setStyle("-fx-background-color:" + color + ";-fx-background-radius:6;-fx-alignment:center;");
            }
        });

        TableColumn<Room, String> colBookings = new TableColumn<>("Active");
        colBookings.setCellValueFactory(d -> {
            long count = Main.reservationService.getAllReservations().stream()
                .filter(r -> r.getRoom().getRoomId().equals(d.getValue().getRoomId()))
                .filter(r -> r.getStatus().name().equals("CONFIRMED"))
                .count();
            return new SimpleStringProperty(String.valueOf(count));
        });
        colBookings.setMaxWidth(65);

        TableColumn<Room, Void> colActions = new TableColumn<>("Actions");
        colActions.setMinWidth(220);
        colActions.setMaxWidth(240);
        colActions.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null); return;
                }
                Room room = getTableView().getItems().get(getIndex());

                Button btnFloor  = actionButton("Floor",  "#cba6f7");
                Button btnStatus = actionButton("Status", "#f9e2af");
                Button btnDelete = actionButton("Delete", "#f38ba8");

                btnFloor.setOnAction(e  -> showAssignFloorDialog(room));
                btnStatus.setOnAction(e -> showChangeStatusDialog(room));
                btnDelete.setOnAction(e -> {
                    Alert c = new Alert(Alert.AlertType.CONFIRMATION,
                        "Delete room " + room.getRoomNumber() + "?", ButtonType.YES, ButtonType.NO);
                    c.showAndWait().ifPresent(b -> {
                        if (b == ButtonType.YES) {
                            Main.roomRepository.delete(room.getRoomId());
                            refreshTabs();
                        }
                    });
                });

                HBox box = new HBox(5, btnFloor, btnStatus, btnDelete);
                box.setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        });

        t.getColumns().addAll(colNum, colFloor, colType, colPrice, colStatus, colBookings, colActions);

        VBox wrapper = new VBox(t);
        VBox.setVgrow(t, Priority.ALWAYS);
        return wrapper;
    }

    // ── Room Info Stage ────────────────────────────────────────────────────────
    private void showRoomInfoDialog(Room room) {
        List<Reservation> reservations = Main.reservationService.getAllReservations().stream()
            .filter(r -> r.getRoom().getRoomId().equals(room.getRoomId()))
            .sorted((a, b) -> b.getCheckInDate().compareTo(a.getCheckInDate()))
            .toList();

        long   confirmed = reservations.stream().filter(r -> r.getStatus().name().equals("CONFIRMED")).count();
        long   completed = reservations.stream().filter(r -> r.getStatus().name().equals("COMPLETED")).count();
        long   cancelled = reservations.stream().filter(r -> r.getStatus().name().equals("CANCELLED")).count();
        double revenue   = reservations.stream()
            .filter(r -> !r.getStatus().name().equals("CANCELLED"))
            .mapToDouble(r -> ChronoUnit.DAYS.between(
                r.getCheckInDate(), r.getCheckOutDate()) * r.getRoom().getPricePerNight()).sum();

        Stage stage = new Stage();
        stage.setTitle("Room " + room.getRoomNumber() + " - Information");
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color:#f8f8f2;");

        // Header
        HBox header = new HBox(16);
        header.setPadding(new Insets(16));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color:#e8f0fe;-fx-background-radius:10;" +
            "-fx-border-color:#89b4fa;-fx-border-radius:10;-fx-border-width:0 0 0 4;");

        Label roomNum = new Label("Room " + room.getRoomNumber());
        roomNum.setFont(Font.font("System", FontWeight.BOLD, 20));
        roomNum.setStyle("-fx-text-fill:#1e1e2e;");

        String floorStr = room.getFloor() != null ? "Floor " + room.getFloor().getFloorNumber() + "  |  " : "";
        Label roomInfo = new Label(floorStr + room.getType().name() + "  |  $" +
            String.format("%.2f", room.getPricePerNight()) + "/night  |  " + room.getStatus().name());
        roomInfo.setFont(Font.font("System", 13));
        roomInfo.setStyle("-fx-text-fill:#6c7086;");
        header.getChildren().add(new VBox(6, roomNum, roomInfo));

        // Stats
        HBox stats = new HBox(12);
        stats.getChildren().addAll(
            summaryCard("Total", String.valueOf(reservations.size()), "#89b4fa"),
            summaryCard("Active",     String.valueOf(confirmed), "#a6e3a1"),
            summaryCard("Completed",  String.valueOf(completed), "#cba6f7"),
            summaryCard("Cancelled",  String.valueOf(cancelled), "#f38ba8"),
            summaryCard("Revenue",    String.format("$%.0f", revenue), "#94e2d5")
        );

        Label histTitle = new Label("Reservation History");
        histTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        histTitle.setStyle("-fx-text-fill:#1e1e2e;");

        javafx.scene.Node histNode = reservations.isEmpty()
            ? new Label("No reservations yet.")
            : buildResTable(reservations);

        Button btnClose = actionButton("Close", "#89b4fa");
        btnClose.setOnAction(e -> stage.close());
        HBox btnRow = new HBox(btnClose);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(header, stats, histTitle, histNode, btnRow);
        stage.setScene(new Scene(root, 700, 520));
        stage.show();
    }

    // ── Reservation history table ──────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private TableView<Reservation> buildResTable(List<Reservation> reservations) {
        TableView<Reservation> t = new TableView<>(FXCollections.observableArrayList(reservations));
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        t.setStyle("-fx-background-color:white;-fx-background-radius:8;");
        t.setPrefHeight(200);

        TableColumn<Reservation, String> colGuest = new TableColumn<>("Guest");
        colGuest.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCustomer().getFullName()));

        TableColumn<Reservation, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCustomer().getEmail()));

        TableColumn<Reservation, String> colIn = new TableColumn<>("Check-in");
        colIn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCheckInDate().format(FMT)));

        TableColumn<Reservation, String> colOut = new TableColumn<>("Check-out");
        colOut.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCheckOutDate().format(FMT)));

        TableColumn<Reservation, String> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(d -> {
            long n = ChronoUnit.DAYS.between(d.getValue().getCheckInDate(), d.getValue().getCheckOutDate());
            return new SimpleStringProperty(String.format("$%.2f", n * d.getValue().getRoom().getPricePerNight()));
        });
        colTotal.setMaxWidth(90);

        TableColumn<Reservation, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
        colStatus.setMaxWidth(100);
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String c = switch (item) {
                    case "CONFIRMED" -> "#a6e3a1"; case "CANCELLED" -> "#f38ba8";
                    case "COMPLETED" -> "#89b4fa"; default -> "#cdd6f4";
                };
                setStyle("-fx-background-color:" + c + ";-fx-background-radius:6;-fx-alignment:center;");
            }
        });

        t.getColumns().addAll(colGuest, colEmail, colIn, colOut, colTotal, colStatus);
        return t;
    }

    // ── Add Floor Dialog ───────────────────────────────────────────────────────
    private void showAddFloorDialog() {
        Dialog<Floor> dialog = new Dialog<>();
        dialog.setTitle("Add New Floor");
        dialog.setHeaderText("Enter floor details");

        ButtonType addBtn = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField tfNumber = new TextField();
        tfNumber.setPromptText("e.g. 1, 2, 3");

        TextField tfDesc = new TextField();
        tfDesc.setPromptText("e.g. Standard Rooms (optional)");

        grid.addRow(0, new Label("Floor Number:"), tfNumber);
        grid.addRow(1, new Label("Description:"),  tfDesc);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> {
            if (btn == addBtn) {
                try {
                    int num = Integer.parseInt(tfNumber.getText().trim());
                    Floor f = new Floor(UUID.randomUUID(), num, tfDesc.getText().trim());
                    return Main.floorRepository.save(f);
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK).showAndWait();
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(f -> refreshTabs());
    }

    // ── Add Room Dialog ────────────────────────────────────────────────────────
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
        tfNumber.setPromptText("e.g. 101");

        ComboBox<RoomType> cbType = new ComboBox<>();
        cbType.getItems().addAll(RoomType.values());
        cbType.setValue(RoomType.SINGLE);

        TextField tfPrice = new TextField("120.00");

        // Floor selection
        ComboBox<String> cbFloor = new ComboBox<>();
        cbFloor.getItems().add("No floor");
        List<Floor> floors = Main.floorRepository.findAll();
        floors.forEach(f -> cbFloor.getItems().add(f.getDisplayName()));
        cbFloor.setValue("No floor");

        grid.addRow(0, new Label("Room Number:"), tfNumber);
        grid.addRow(1, new Label("Type:"),        cbType);
        grid.addRow(2, new Label("Price/Night:"), tfPrice);
        grid.addRow(3, new Label("Floor:"),       cbFloor);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> {
            if (btn == addBtn) {
                try {
                    String num    = tfNumber.getText().trim();
                    RoomType type = cbType.getValue();
                    double price  = Double.parseDouble(tfPrice.getText().trim());

                    // Determine selected floor
                    Floor selectedFloor = null;
                    int selIdx = cbFloor.getSelectionModel().getSelectedIndex();
                    if (selIdx > 0) {
                        selectedFloor = floors.get(selIdx - 1);
                    }

                    Room room = new Room(UUID.randomUUID(), num, type, price,
                                        RoomStatus.AVAILABLE, selectedFloor);
                    return Main.roomRepository.save(room);
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK).showAndWait();
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(r -> refreshTabs());
    }

    // ── Assign Floor Dialog ────────────────────────────────────────────────────
    private void showAssignFloorDialog(Room room) {
        Dialog<Floor> dialog = new Dialog<>();
        dialog.setTitle("Assign Floor");
        dialog.setHeaderText("Room " + room.getRoomNumber() + " - assign to floor");

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        List<Floor> floors = Main.floorRepository.findAll();
        if (floors.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION,
                "No floors exist yet. Please add a floor first.", ButtonType.OK).showAndWait();
            return;
        }

        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().add("No floor (unassign)");
        floors.forEach(f -> cb.getItems().add(f.getDisplayName()));

        // Pre-select current floor
        if (room.getFloor() != null) {
            for (int i = 0; i < floors.size(); i++) {
                if (floors.get(i).getFloorId().equals(room.getFloor().getFloorId())) {
                    cb.getSelectionModel().select(i + 1);
                    break;
                }
            }
        } else {
            cb.getSelectionModel().select(0);
        }

        VBox box = new VBox(10, new Label("Select Floor:"), cb);
        box.setPadding(new Insets(20));
        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                int idx = cb.getSelectionModel().getSelectedIndex();
                return idx > 0 ? floors.get(idx - 1) : null;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newFloor -> {
            room.setFloor(newFloor);
            Main.roomRepository.save(room);
            refreshTabs();
        });

        // Handle "No floor" case (result is null but OK was pressed)
        if (dialog.getResult() == null && cb.getSelectionModel().getSelectedIndex() == 0) {
            room.setFloor(null);
            Main.roomRepository.save(room);
            refreshTabs();
        }
    }

    // ── Change Status Dialog ───────────────────────────────────────────────────
    private void showChangeStatusDialog(Room room) {
        Dialog<RoomStatus> dialog = new Dialog<>();
        dialog.setTitle("Change Status");
        dialog.setHeaderText("Room " + room.getRoomNumber() + " - current: " + room.getStatus());

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
            refreshTabs();
        });
    }

    // ── Summary card ───────────────────────────────────────────────────────────
    private VBox summaryCard(String label, String value, String color) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color:white;-fx-background-radius:8;" +
            "-fx-border-color:" + color + ";-fx-border-radius:8;-fx-border-width:2;");
        Label val = new Label(value);
        val.setFont(Font.font("System", FontWeight.BOLD, 20));
        val.setStyle("-fx-text-fill:#1e1e2e;");
        Label lbl = new Label(label);
        lbl.setFont(Font.font("System", 11));
        lbl.setStyle("-fx-text-fill:#6c7086;");
        card.getChildren().addAll(val, lbl);
        return card;
    }

    private Button actionButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:" + color + ";-fx-background-radius:6;" +
            "-fx-cursor:hand;-fx-font-size:12;-fx-padding:5 12 5 12;");
        return btn;
    }
}
