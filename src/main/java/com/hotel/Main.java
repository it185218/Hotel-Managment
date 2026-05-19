package com.hotel;

import com.hotel.db.DatabaseConnection;
import com.hotel.db.DatabaseInitializer;
import com.hotel.repository.*;
import com.hotel.service.*;
import com.hotel.ui.MainWindow;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

public class Main extends Application {

    public static RoomService             roomService;
    public static ReservationService      reservationService;
    public static MySQLCustomerRepository customerRepository;
    public static MySQLRoomRepository     roomRepository;

    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialise DB schema (creates tables if not exist)
            DatabaseInitializer.initialize();

            // Wire MySQL repositories
            MySQLRoomRepository        roomRepo        = new MySQLRoomRepository();
            MySQLCustomerRepository    customerRepo    = new MySQLCustomerRepository();
            MySQLReservationRepository reservationRepo = new MySQLReservationRepository();

            roomRepository     = roomRepo;
            customerRepository = customerRepo;

            roomService        = new RoomService(roomRepo, reservationRepo);
            reservationService = new ReservationService(reservationRepo, roomRepo, customerRepo);

            new MainWindow(primaryStage).show();

        } catch (Exception e) {
            // Show a friendly error dialog if DB connection fails
            Alert alert = new Alert(Alert.AlertType.ERROR,
                "Cannot connect to MySQL database.\n\n" +
                "Please check:\n" +
                "  1. MySQL Server is running\n" +
                "  2. Password in src/main/resources/db.properties is correct\n\n" +
                "Error: " + e.getMessage(),
                ButtonType.OK);
            alert.setTitle("Database Connection Error");
            alert.setHeaderText("Failed to connect to MySQL");
            alert.showAndWait();
            System.exit(1);
        }
    }

    @Override
    public void stop() {
        // Close DB connection cleanly when app exits
        DatabaseConnection.getInstance().close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
