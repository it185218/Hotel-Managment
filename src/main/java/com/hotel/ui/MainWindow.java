package com.hotel.ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class MainWindow {

    private final Stage stage;
    private BorderPane root;
    private StackPane contentArea;

    private static final String SIDEBAR_BG   = "#1e1e2e";
    private static final String SIDEBAR_TEXT = "#cdd6f4";
    private static final String ACTIVE_BG    = "#313244";
    private static final String ACCENT       = "#89b4fa";

    public MainWindow(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        root = new BorderPane();
        root.setLeft(buildSidebar());
        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #f8f8f2;");
        root.setCenter(contentArea);

        showDashboard();

        Scene scene = new Scene(root, 1100, 680);
        stage.setTitle("Hotel Management System");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(210);
        sidebar.setStyle("-fx-background-color: " + SIDEBAR_BG + ";");
        sidebar.setPadding(new Insets(0));

        // Logo
        VBox logoBox = new VBox(2);
        logoBox.setPadding(new Insets(20, 16, 16, 16));
        logoBox.setStyle("-fx-border-color: #313244; -fx-border-width: 0 0 1 0;");
        Label title = new Label("Hotel MS");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        title.setTextFill(Color.web(SIDEBAR_TEXT));
        Label subtitle = new Label("Management System");
        subtitle.setFont(Font.font("System", 11));
        subtitle.setTextFill(Color.web("#6c7086"));
        logoBox.getChildren().addAll(title, subtitle);

        // Nav items
        Button btnDashboard    = navButton("Dashboard",    "D");
        Button btnRooms        = navButton("Rooms",        "R");
        Button btnCustomers    = navButton("Customers",    "C");
        Button btnReservations = navButton("Reservations", "B");

        btnDashboard.setOnAction(e -> {
            setActive(btnDashboard, btnRooms, btnCustomers, btnReservations);
            showDashboard();
        });
        btnRooms.setOnAction(e -> {
            setActive(btnRooms, btnDashboard, btnCustomers, btnReservations);
            showRooms();
        });
        btnCustomers.setOnAction(e -> {
            setActive(btnCustomers, btnDashboard, btnRooms, btnReservations);
            showCustomers();
        });
        btnReservations.setOnAction(e -> {
            setActive(btnReservations, btnDashboard, btnRooms, btnCustomers);
            showReservations();
        });

        setActive(btnDashboard, btnRooms, btnCustomers, btnReservations);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        sidebar.getChildren().addAll(logoBox, btnDashboard, btnRooms, btnCustomers, btnReservations, spacer);
        return sidebar;
    }

    private Button navButton(String text, String icon) {
        Button btn = new Button("  " + icon + "  " + text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPadding(new Insets(12, 16, 12, 16));
        btn.setFont(Font.font("System", 13));
        btn.setTextFill(Color.web(SIDEBAR_TEXT));
        btn.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-cursor: hand;" +
            "-fx-alignment: center-left;" +
            "-fx-border-width: 0;"
        );
        btn.setOnMouseEntered(e -> {
            if (!btn.getStyle().contains(ACTIVE_BG))
                btn.setStyle(btn.getStyle().replace("-fx-background-color: transparent;",
                    "-fx-background-color: #27273a;"));
        });
        btn.setOnMouseExited(e -> {
            if (!btn.getStyle().contains(ACTIVE_BG))
                btn.setStyle(btn.getStyle().replace("-fx-background-color: #27273a;",
                    "-fx-background-color: transparent;"));
        });
        return btn;
    }

    private void setActive(Button active, Button... others) {
        active.setStyle(
            "-fx-background-color: " + ACTIVE_BG + ";" +
            "-fx-cursor: hand;" +
            "-fx-alignment: center-left;" +
            "-fx-border-color: " + ACCENT + ";" +
            "-fx-border-width: 0 0 0 3;"
        );
        active.setTextFill(Color.web(ACCENT));
        for (Button b : others) {
            b.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-cursor: hand;" +
                "-fx-alignment: center-left;" +
                "-fx-border-width: 0;"
            );
            b.setTextFill(Color.web(SIDEBAR_TEXT));
        }
    }

    private void showDashboard() {
        contentArea.getChildren().setAll(new DashboardView().build());
    }

    private void showRooms() {
        contentArea.getChildren().setAll(new RoomsView().build());
    }

    private void showCustomers() {
        contentArea.getChildren().setAll(new CustomersView().build());
    }

    private void showReservations() {
        contentArea.getChildren().setAll(new ReservationsView().build());
    }
}
