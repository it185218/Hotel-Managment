package com.hotel.ui;

import com.hotel.Main;
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

public class RoomsView {

    private ObservableList<Room> roomList;
    private TableView<Room> table;

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
        bar.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");

        Label title = new Label("Rooms");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        title.setTextFill(Color.web("#1e1e2e"));

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
        table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        content.getChildren().add(table);
        return content;
    }

    @SuppressWarnings("unchecked")
    private TableView<Room> buildTable() {
        TableView<Room> t = new TableView<>(roomList);
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        t.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        TableColumn<Room, String> colNum = new TableColumn<>("Room #");
        colNum.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRoomNumber()));
        colNum.setMaxWidth(90);

        TableColumn<Room, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType().name()));

        TableColumn<Room, String> colPrice = new TableColumn<>("Price / Night");
        colPrice.setCellValueFactory(d ->
            new SimpleStringProperty(String.format("$%.2f", d.getValue().getPricePerNight())));

        TableColumn<Room, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
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

        TableColumn<Room, Void> colActions = new TableColumn<>("Actions");
        colActions.setCellFactory(col -> new TableCell<>() {
            final Button btnEdit   = actionButton("Status", "#cba6f7");
            final Button btnDelete = actionButton("Delete", "#f38ba8");
            final HBox   box       = new HBox(6, btnEdit, btnDelete);
            {
                box.setAlignment(Pos.CENTER);
                btnEdit.setOnAction(e -> {
                    Room r = getTableView().getItems().get(getIndex());
                    showChangeStatusDialog(r);
                });
                btnDelete.setOnAction(e -> {
                    Room r = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Delete room " + r.getRoomNumber() + "?", ButtonType.YES, ButtonType.NO);
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

        t.getColumns().addAll(colNum, colType, colPrice, colStatus, colActions);
        return t;
    }

    private void showAddRoomDialog() {
        Dialog<Room> dialog = new Dialog<>();
        dialog.setTitle("Add New Room");
        dialog.setHeaderText("Enter room details");

        ButtonType addBtn = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
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
                    String num   = tfNumber.getText().trim();
                    RoomType type = cbType.getValue();
                    double price  = Double.parseDouble(tfPrice.getText().trim());
                    return Main.roomService.createRoom(num, type, price);
                } catch (Exception ex) {
                    showError("Invalid input: " + ex.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(room -> {
            roomList.setAll(Main.roomService.getAllRooms());
        });
    }

    private void showChangeStatusDialog(Room room) {
        Dialog<RoomStatus> dialog = new Dialog<>();
        dialog.setTitle("Change Status");
        dialog.setHeaderText("Room " + room.getRoomNumber() + " — current: " + room.getStatus());

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
        btn.setStyle("-fx-background-color:" + color + ";-fx-background-radius:6;-fx-cursor:hand;" +
                     "-fx-font-size:12;-fx-padding:5 12 5 12;");
        return btn;
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
