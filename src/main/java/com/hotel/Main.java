package com.hotel;

import com.hotel.repository.*;
import com.hotel.service.*;
import com.hotel.ui.MainWindow;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    public static RoomService        roomService;
    public static ReservationService reservationService;
    public static CustomerRepository customerRepository;

    @Override
    public void start(Stage primaryStage) {
        RoomRepository        roomRepo        = new InMemoryRoomRepository();
        CustomerRepository    customerRepo    = new InMemoryCustomerRepository();
        ReservationRepository reservationRepo = new InMemoryReservationRepository();

        roomService        = new RoomService(roomRepo, reservationRepo);
        reservationService = new ReservationService(reservationRepo, roomRepo, customerRepo);
        customerRepository = customerRepo;

        new MainWindow(primaryStage).show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
