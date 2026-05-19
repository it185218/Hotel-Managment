package com.hotel.ui;

import com.hotel.Main;
import com.hotel.domain.Customer;
import com.hotel.domain.Reservation;
import com.hotel.domain.Room;
import com.hotel.enums.ReservationStatus;
import com.hotel.exception.ReservationConflictException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class ReservationsView {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yy");

    // Booking wizard state
    private Room           selectedRoom;
    private Customer       selectedCustomer;
    private LocalDate      checkIn;
    private LocalDate      checkOut;

    // Calendar state
    private YearMonth      calendarMonth = YearMonth.now();
    private Set<LocalDate> bookedDates   = new HashSet<>();

    // Main layout panels
    private BorderPane root;
    private VBox       leftPanel;   // rooms grid + reservations table
    private VBox       rightPanel;  // wizard steps

    public VBox build() {
        VBox wrapper = new VBox(0);
        wrapper.setStyle("-fx-background-color:#f8f8f2;");

        wrapper.getChildren().addAll(buildTopBar(), buildBody());
        return wrapper;
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

        bar.getChildren().add(title);
        return bar;
    }

    // ── Body: left rooms + right wizard ───────────────────────────────────────
    private HBox buildBody() {
        HBox body = new HBox(0);
        VBox.setVgrow(body, Priority.ALWAYS);

        // LEFT: rooms grid on top, reservations table below
        leftPanel = new VBox(0);
        leftPanel.setPrefWidth(520);
        leftPanel.setMinWidth(400);
        leftPanel.setStyle("-fx-background-color:#f8f8f2;-fx-border-color:#e0e0e0;-fx-border-width:0 1 0 0;");

        refreshLeftPanel();

        // RIGHT: wizard panel
        rightPanel = new VBox(0);
        rightPanel.setStyle("-fx-background-color:#ffffff;");
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        showWizardStep1();

        body.getChildren().addAll(leftPanel, rightPanel);
        return body;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LEFT PANEL
    // ═══════════════════════════════════════════════════════════════════════════

    private void refreshLeftPanel() {
        leftPanel.getChildren().clear();
        leftPanel.getChildren().addAll(buildRoomsGrid(), buildReservationsTable());
    }

    private VBox buildRoomsGrid() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color:#f8f8f2;");

        Label lbl = new Label("Select a Room to Book");
        lbl.setFont(Font.font("System", FontWeight.BOLD, 13));
        lbl.setTextFill(Color.web("#6c7086"));

        FlowPane grid = new FlowPane(10, 10);
        grid.setPrefWrapLength(480);

        List<Room> rooms = Main.roomService.getAllRooms();
        for (Room room : rooms) {
            grid.getChildren().add(buildRoomCard(room));
        }

        box.getChildren().addAll(lbl, grid);
        return box;
    }

    private VBox buildRoomCard(Room room) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(12));
        card.setPrefWidth(140);
        card.setAlignment(Pos.CENTER);
        card.setCursor(Cursor.HAND);

        // Color based on status
        String bg, border;
        switch (room.getStatus()) {
            case MAINTENANCE -> { bg = "#fff0f0"; border = "#f38ba8"; }
            case OCCUPIED    -> { bg = "#fffbe6"; border = "#f9e2af"; }
            default          -> { bg = "#f0fff4"; border = "#a6e3a1"; }
        }

        boolean isSelected = selectedRoom != null &&
                             selectedRoom.getRoomId().equals(room.getRoomId());

        if (isSelected) {
            bg = "#e8f0fe"; border = "#89b4fa";
        }

        card.setStyle(
            "-fx-background-color:" + bg + ";" +
            "-fx-background-radius:10;" +
            "-fx-border-color:" + border + ";" +
            "-fx-border-radius:10;" +
            "-fx-border-width:" + (isSelected ? "2" : "1") + ";"
        );

        Label num = new Label("Room " + room.getRoomNumber());
        num.setFont(Font.font("System", FontWeight.BOLD, 14));
        num.setTextFill(Color.web("#1e1e2e"));

        Label type = new Label(room.getType().name());
        type.setFont(Font.font("System", 11));
        type.setTextFill(Color.web("#6c7086"));

        Label price = new Label(String.format("$%.0f/night", room.getPricePerNight()));
        price.setFont(Font.font("System", FontWeight.BOLD, 12));
        price.setTextFill(Color.web("#40a02b"));

        Label status = new Label(room.getStatus().name());
        status.setFont(Font.font("System", 10));
        status.setTextFill(Color.web(border));
        status.setStyle(
            "-fx-background-color:" + bg + ";" +
            "-fx-background-radius:4;" +
            "-fx-border-color:" + border + ";" +
            "-fx-border-radius:4;" +
            "-fx-padding:2 6 2 6;"
        );

        card.getChildren().addAll(num, type, price, status);

        card.setOnMouseClicked(e -> {
            if (room.getStatus().name().equals("MAINTENANCE")) {
                showError("Room " + room.getRoomNumber() + " is under maintenance.");
                return;
            }
            selectedRoom    = room;
            selectedCustomer = null;
            checkIn         = null;
            checkOut        = null;
            refreshLeftPanel();
            loadBookedDates(room);
            showWizardStep2(); // pick customer
        });

        card.setOnMouseEntered(e -> {
            if (!isSelected) card.setStyle(card.getStyle().replace(
                "-fx-border-width:" + (isSelected ? "2" : "1"),
                "-fx-border-width:2"));
        });
        card.setOnMouseExited(e -> {
            if (!isSelected) card.setStyle(card.getStyle().replace(
                "-fx-border-width:2",
                "-fx-border-width:1"));
        });

        return card;
    }

    private VBox buildReservationsTable() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(0, 16, 16, 16));
        VBox.setVgrow(box, Priority.ALWAYS);

        // Filter bar
        HBox filterBar = new HBox(8);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label("Reservations");
        lbl.setFont(Font.font("System", FontWeight.BOLD, 13));
        lbl.setTextFill(Color.web("#6c7086"));

        ComboBox<Customer> cbGuest = new ComboBox<>();
        cbGuest.setPromptText("All guests");
        cbGuest.setPrefWidth(160);
        List<Customer> allCustomers = Main.customerRepository.findAll();
        cbGuest.setItems(FXCollections.observableArrayList(allCustomers));
        cbGuest.setCellFactory(lv -> customerCell());
        cbGuest.setButtonCell(customerCell());

        ComboBox<String> cbStatus = new ComboBox<>();
        cbStatus.setItems(FXCollections.observableArrayList("All", "CONFIRMED", "CANCELLED", "COMPLETED"));
        cbStatus.setValue("All");
        cbStatus.setPrefWidth(120);

        Button btnClear = styledButton("Clear", "#6c7086");
        btnClear.setStyle(btnClear.getStyle() + "-fx-text-fill:white;");

        ObservableList<Reservation> resList =
            FXCollections.observableArrayList(Main.reservationService.getAllReservations());

        TableView<Reservation> table = buildTable(resList);
        VBox.setVgrow(table, Priority.ALWAYS);

        cbGuest.valueProperty().addListener((obs, o, customer) ->
            applyFilters(resList, cbGuest.getValue(), cbStatus.getValue()));
        cbStatus.valueProperty().addListener((obs, o, status) ->
            applyFilters(resList, cbGuest.getValue(), cbStatus.getValue()));

        btnClear.setOnAction(e -> {
            cbGuest.setValue(null);
            cbStatus.setValue("All");
            resList.setAll(Main.reservationService.getAllReservations());
        });

        filterBar.getChildren().addAll(lbl, cbGuest, cbStatus, btnClear);
        box.getChildren().addAll(filterBar, table);
        return box;
    }

    private void applyFilters(ObservableList<Reservation> list,
                               Customer customer, String status) {
        List<Reservation> all = Main.reservationService.getAllReservations();
        list.setAll(all.stream()
            .filter(r -> customer == null ||
                r.getCustomer().getCustomerId().equals(customer.getCustomerId()))
            .filter(r -> "All".equals(status) || r.getStatus().name().equals(status))
            .collect(Collectors.toList()));
    }

    @SuppressWarnings("unchecked")
    private TableView<Reservation> buildTable(ObservableList<Reservation> resList) {
        TableView<Reservation> t = new TableView<>(resList);
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        t.setStyle("-fx-background-color:white;-fx-background-radius:8;");
        t.setMaxHeight(260);

        TableColumn<Reservation, String> colGuest = new TableColumn<>("Guest");
        colGuest.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getCustomer().getFullName()));

        TableColumn<Reservation, String> colRoom = new TableColumn<>("Room");
        colRoom.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getRoom().getRoomNumber()));
        colRoom.setMaxWidth(65);

        TableColumn<Reservation, String> colIn = new TableColumn<>("Check-in");
        colIn.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getCheckInDate().format(FMT)));

        TableColumn<Reservation, String> colOut = new TableColumn<>("Check-out");
        colOut.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getCheckOutDate().format(FMT)));

        TableColumn<Reservation, String> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(d -> {
            long n = ChronoUnit.DAYS.between(
                d.getValue().getCheckInDate(), d.getValue().getCheckOutDate());
            return new SimpleStringProperty(
                String.format("$%.0f", n * d.getValue().getRoom().getPricePerNight()));
        });
        colTotal.setMaxWidth(75);

        TableColumn<Reservation, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getStatus().name()));
        colStatus.setMaxWidth(100);
        colStatus.setCellFactory(col -> statusCell());

        TableColumn<Reservation, Void> colActions = new TableColumn<>("");
        colActions.setMaxWidth(140);
        colActions.setCellFactory(col -> new TableCell<>() {
            final Button btnCancel   = styledButton("Cancel",   "#f38ba8");
            final Button btnComplete = styledButton("Done",     "#89b4fa");
            final HBox   box         = new HBox(4, btnCancel, btnComplete);
            {
                box.setAlignment(Pos.CENTER);
                btnCancel.setOnAction(e -> {
                    Reservation r = getTableView().getItems().get(getIndex());
                    if (r.getStatus() != ReservationStatus.CONFIRMED) {
                        showError("Only CONFIRMED reservations can be cancelled."); return;
                    }
                    Main.reservationService.cancelReservation(r.getReservationId());
                    refreshLeftPanel();
                });
                btnComplete.setOnAction(e -> {
                    Reservation r = getTableView().getItems().get(getIndex());
                    if (r.getStatus() != ReservationStatus.CONFIRMED) {
                        showError("Only CONFIRMED reservations can be completed."); return;
                    }
                    Main.reservationService.completeReservation(r.getReservationId());
                    refreshLeftPanel();
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        t.getColumns().addAll(colGuest, colRoom, colIn, colOut, colTotal, colStatus, colActions);
        return t;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RIGHT PANEL - WIZARD
    // ═══════════════════════════════════════════════════════════════════════════

    /** Step 1: prompt user to select a room */
    private void showWizardStep1() {
        rightPanel.getChildren().clear();

        VBox center = new VBox(16);
        center.setAlignment(Pos.CENTER);
        VBox.setVgrow(center, Priority.ALWAYS);

        Label icon = new Label("[H]");
        icon.setFont(Font.font("System", 48));

        Label msg = new Label("Select a room on the left\nto start a new booking");
        msg.setFont(Font.font("System", 14));
        msg.setTextFill(Color.web("#6c7086"));
        msg.setTextAlignment(TextAlignment.CENTER);

        center.getChildren().addAll(icon, msg);
        rightPanel.getChildren().add(center);
    }

    /** Step 2: pick customer */
    private void showWizardStep2() {
        rightPanel.getChildren().clear();

        VBox panel = new VBox(20);
        panel.setPadding(new Insets(24));
        VBox.setVgrow(panel, Priority.ALWAYS);

        // Header
        Label header = new Label("Step 1 of 2 - Select Guest");
        header.setFont(Font.font("System", FontWeight.BOLD, 15));
        header.setStyle("-fx-text-fill:#1e1e2e;");

        Label subheader = new Label("Room " + selectedRoom.getRoomNumber() +
            " | " + selectedRoom.getType().name() +
            " | $" + String.format("%.0f", selectedRoom.getPricePerNight()) + "/night");
        subheader.setFont(Font.font("System", 12));
        subheader.setStyle("-fx-text-fill:#6c7086;");

        Separator sep = new Separator();

        // Customer list
        Label lbl = new Label("Choose a customer:");
        lbl.setFont(Font.font("System", 13));
        lbl.setTextFill(Color.web("#1e1e2e"));

        VBox customerList = new VBox(8);
        List<Customer> customers = Main.customerRepository.findAll();

        if (customers.isEmpty()) {
            Label empty = new Label("No customers found.\nPlease add a customer first.");
            empty.setTextFill(Color.web("#f38ba8"));
            customerList.getChildren().add(empty);
        } else {
            for (Customer c : customers) {
                HBox row = buildCustomerRow(c);
                customerList.getChildren().add(row);
            }
        }

        ScrollPane scroll = new ScrollPane(customerList);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Button btnBack = styledButton("<< Back", "#6c7086");
        btnBack.setStyle(btnBack.getStyle() + "-fx-text-fill:white;");
        btnBack.setOnAction(e -> {
            selectedRoom = null;
            refreshLeftPanel();
            showWizardStep1();
        });

        panel.getChildren().addAll(header, subheader, sep, lbl, scroll, btnBack);
        rightPanel.getChildren().add(panel);
    }

    private HBox buildCustomerRow(Customer c) {
        HBox row = new HBox(12);
        row.setPadding(new Insets(10, 14, 10, 14));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setCursor(Cursor.HAND);
        row.setStyle(
            "-fx-background-color:#f8f8f2;" +
            "-fx-background-radius:8;" +
            "-fx-border-color:#e0e0e0;" +
            "-fx-border-radius:8;"
        );

        Label avatar = new Label(String.valueOf(c.getFirstName().charAt(0)).toUpperCase());
        avatar.setMinSize(36, 36);
        avatar.setMaxSize(36, 36);
        avatar.setAlignment(Pos.CENTER);
        avatar.setFont(Font.font("System", FontWeight.BOLD, 16));
        avatar.setStyle("-fx-background-color:#89b4fa;-fx-background-radius:18;-fx-text-fill:white;");

        VBox info = new VBox(2);
        Label name = new Label(c.getFullName());
        name.setFont(Font.font("System", FontWeight.BOLD, 13));
        name.setStyle("-fx-text-fill: #1e1e2e;");
        Label email = new Label(c.getEmail());
        email.setFont(Font.font("System", 11));
        email.setStyle("-fx-text-fill: #6c7086;");
        info.getChildren().addAll(name, email);

        row.getChildren().addAll(avatar, info);

        row.setOnMouseEntered(e -> {
            row.setStyle("-fx-background-color:#e8f0fe;-fx-background-radius:8;-fx-border-color:#89b4fa;-fx-border-radius:8;");
            name.setStyle("-fx-text-fill:#1e1e2e;");
            email.setStyle("-fx-text-fill:#6c7086;");
        });
        row.setOnMouseExited(e -> {
            row.setStyle("-fx-background-color:#f8f8f2;-fx-background-radius:8;-fx-border-color:#e0e0e0;-fx-border-radius:8;");
            name.setStyle("-fx-text-fill:#1e1e2e;");
            email.setStyle("-fx-text-fill:#6c7086;");
        });
        row.setOnMouseClicked(e -> {
            selectedCustomer = c;
            checkIn  = null;
            checkOut = null;
            showWizardStep3();
        });

        return row;
    }

    /** Step 3: calendar with blocked dates */
    private void showWizardStep3() {
        rightPanel.getChildren().clear();

        VBox panel = new VBox(16);
        panel.setPadding(new Insets(24));
        VBox.setVgrow(panel, Priority.ALWAYS);

        Label header = new Label("Step 2 of 2 - Pick Dates");
        header.setFont(Font.font("System", FontWeight.BOLD, 15));
        header.setStyle("-fx-text-fill:#1e1e2e;");

        Label subheader = new Label(
            "Room " + selectedRoom.getRoomNumber() + "  |  Guest: " + selectedCustomer.getFullName());
        subheader.setFont(Font.font("System", 12));
        subheader.setStyle("-fx-text-fill:#6c7086;");

        Separator sep = new Separator();

        // Legend
        HBox legend = new HBox(16);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.getChildren().addAll(
            legendDot("#a6e3a1", "Available"),
            legendDot("#f38ba8", "Booked"),
            legendDot("#89b4fa", "Selected")
        );

        // Calendar
        VBox calendarBox = new VBox(0);
        calendarBox.setStyle(
            "-fx-background-color:white;-fx-background-radius:10;" +
            "-fx-border-color:#e0e0e0;-fx-border-radius:10;");
        refreshCalendar(calendarBox);

        // Cost preview
        Label lblCost = new Label("");
        lblCost.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblCost.setTextFill(Color.web("#40a02b"));

        // Buttons
        HBox buttons = new HBox(10);
        Button btnBack   = styledButton("<< Back",    "#6c7086");
        Button btnConfirm = styledButton("Confirm Booking", "#a6e3a1");
        btnBack.setStyle(btnBack.getStyle() + "-fx-text-fill:white;");
        btnConfirm.setDisable(true);
        buttons.getChildren().addAll(btnBack, btnConfirm);

        // Update cost & confirm button when dates change
        Runnable updateCost = () -> {
            if (checkIn != null && checkOut != null) {
                long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
                double total = nights * selectedRoom.getPricePerNight();
                lblCost.setText(String.format(
                    "%s → %s   |   %d nights   |   Total: $%.2f",
                    checkIn.format(FMT), checkOut.format(FMT), nights, total));
                btnConfirm.setDisable(false);
            } else if (checkIn != null) {
                lblCost.setText("Check-in: " + checkIn.format(FMT) + "  - now select check-out date");
                lblCost.setTextFill(Color.web("#89b4fa"));
                btnConfirm.setDisable(true);
            }
            refreshCalendar(calendarBox);
        };

        // Store reference so calendar cells can call it
        this.costUpdater = updateCost;

        btnBack.setOnAction(e -> showWizardStep2());
        btnConfirm.setOnAction(e -> confirmBooking(lblCost));

        panel.getChildren().addAll(
            header, subheader, sep, legend, calendarBox, lblCost, buttons);
        rightPanel.getChildren().add(panel);
    }

    // Runnable reference so calendar cells can trigger cost update
    private Runnable costUpdater;

    private void refreshCalendar(VBox calendarBox) {
        calendarBox.getChildren().clear();
        calendarBox.getChildren().add(buildCalendar());
    }

    private VBox buildCalendar() {
        VBox cal = new VBox(0);

        // Month navigation header
        HBox nav = new HBox(10);
        nav.setPadding(new Insets(10, 14, 10, 14));
        nav.setAlignment(Pos.CENTER);
        nav.setStyle("-fx-background-color:#1e1e2e;-fx-background-radius:10 10 0 0;");

        Button btnPrev = new Button("‹");
        Button btnNext = new Button("›");
        for (Button b : new Button[]{btnPrev, btnNext}) {
            b.setStyle("-fx-background-color:transparent;-fx-text-fill:white;" +
                       "-fx-font-size:18;-fx-cursor:hand;-fx-padding:0 10 0 10;");
        }

        Label monthLabel = new Label(
            calendarMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            + " " + calendarMonth.getYear());
        monthLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        monthLabel.setTextFill(Color.WHITE);
        HBox.setHgrow(monthLabel, Priority.ALWAYS);
        monthLabel.setAlignment(Pos.CENTER);

        btnPrev.setOnAction(e -> {
            calendarMonth = calendarMonth.minusMonths(1);
            if (costUpdater != null) costUpdater.run();
        });
        btnNext.setOnAction(e -> {
            calendarMonth = calendarMonth.plusMonths(1);
            if (costUpdater != null) costUpdater.run();
        });

        nav.getChildren().addAll(btnPrev, monthLabel, btnNext);

        // Day-of-week headers
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(8));
        grid.setHgap(4);
        grid.setVgap(4);

        String[] days = {"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"};
        for (int i = 0; i < 7; i++) {
            Label d = new Label(days[i]);
            d.setMinSize(36, 28);
            d.setAlignment(Pos.CENTER);
            d.setFont(Font.font("System", FontWeight.BOLD, 11));
            d.setTextFill(Color.web("#6c7086"));
            grid.add(d, i, 0);
        }

        // Days grid
        LocalDate first     = calendarMonth.atDay(1);
        int       startCol  = first.getDayOfWeek().getValue() - 1; // Mon=0
        int       daysInMonth = calendarMonth.lengthOfMonth();
        LocalDate today     = LocalDate.now();

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = calendarMonth.atDay(day);
            int col = (startCol + day - 1) % 7;
            int row = (startCol + day - 1) / 7 + 1;

            Label cell = buildDayCell(date, today);
            grid.add(cell, col, row);
        }

        cal.getChildren().addAll(nav, grid);
        return cal;
    }

    private Label buildDayCell(LocalDate date, LocalDate today) {
        Label cell = new Label(String.valueOf(date.getDayOfMonth()));
        cell.setMinSize(36, 36);
        cell.setMaxSize(36, 36);
        cell.setAlignment(Pos.CENTER);
        cell.setFont(Font.font("System", 12));

        boolean isBooked   = isDateBooked(date);
        boolean isPast     = date.isBefore(today);
        boolean isCheckIn  = date.equals(checkIn);
        boolean isCheckOut = date.equals(checkOut);
        boolean inRange    = checkIn != null && checkOut != null &&
                             date.isAfter(checkIn) && date.isBefore(checkOut);

        if (isPast) {
            // Past dates - greyed out, not clickable
            cell.setStyle("-fx-background-color:#f0f0f0;-fx-background-radius:18;");
            cell.setTextFill(Color.web("#c0c0c0"));

        } else if (isBooked) {
            // Booked - red, not clickable
            cell.setStyle(
                "-fx-background-color:#f38ba8;-fx-background-radius:18;");
            cell.setTextFill(Color.WHITE);
            Tooltip.install(cell, new Tooltip("Already booked"));

        } else if (isCheckIn || isCheckOut) {
            // Selected check-in or check-out
            cell.setStyle("-fx-background-color:#89b4fa;-fx-background-radius:18;");
            cell.setTextFill(Color.WHITE);
            cell.setCursor(Cursor.HAND);

        } else if (inRange) {
            // In selected range
            cell.setStyle("-fx-background-color:#d4e6ff;-fx-background-radius:0;");
            cell.setTextFill(Color.web("#1e1e2e"));
            cell.setCursor(Cursor.HAND);

        } else {
            // Available
            cell.setStyle("-fx-background-color:#f0fff4;-fx-background-radius:18;");
            cell.setTextFill(Color.web("#1e1e2e"));
            cell.setCursor(Cursor.HAND);

            cell.setOnMouseEntered(e -> cell.setStyle(
                "-fx-background-color:#a6e3a1;-fx-background-radius:18;"));
            cell.setOnMouseExited(e -> cell.setStyle(
                "-fx-background-color:#f0fff4;-fx-background-radius:18;"));
        }

        // Click logic: first click = check-in, second click = check-out
        if (!isPast && !isBooked) {
            cell.setOnMouseClicked(e -> {
                if (checkIn == null || (checkOut != null)) {
                    // Start fresh selection
                    checkIn  = date;
                    checkOut = null;
                } else {
                    // Second click: must be after check-in and no booked dates in range
                    if (date.isAfter(checkIn)) {
                        if (hasBookedDateInRange(checkIn, date)) {
                            showError("Your selected range includes already-booked dates.\nPlease choose different dates.");
                            checkIn  = null;
                            checkOut = null;
                        } else {
                            checkOut = date;
                        }
                    } else {
                        // Clicked before check-in: restart
                        checkIn  = date;
                        checkOut = null;
                    }
                }
                if (costUpdater != null) costUpdater.run();
            });
        }

        return cell;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BOOKING HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private void loadBookedDates(Room room) {
        bookedDates.clear();
        Main.reservationService.getAllReservations().stream()
            .filter(r -> r.getRoom().getRoomId().equals(room.getRoomId()))
            .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
            .forEach(r -> {
                LocalDate d = r.getCheckInDate();
                while (d.isBefore(r.getCheckOutDate())) {
                    bookedDates.add(d);
                    d = d.plusDays(1);
                }
            });
    }

    private boolean isDateBooked(LocalDate date) {
        return bookedDates.contains(date);
    }

    private boolean hasBookedDateInRange(LocalDate from, LocalDate to) {
        LocalDate d = from.plusDays(1);
        while (d.isBefore(to)) {
            if (bookedDates.contains(d)) return true;
            d = d.plusDays(1);
        }
        return false;
    }

    private void confirmBooking(Label lblCost) {
        if (checkIn == null || checkOut == null) {
            showError("Please select both check-in and check-out dates.");
            return;
        }
        try {
            Reservation res = Main.reservationService.createReservation(
                selectedCustomer.getCustomerId(),
                selectedRoom.getRoomId(),
                checkIn, checkOut);

            long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
            double total = nights * selectedRoom.getPricePerNight();

            showInfo(String.format(
                "Booking Confirmed!\n\n" +
                "Guest:      %s\n" +
                "Room:       %s (%s)\n" +
                "Check-in:   %s\n" +
                "Check-out:  %s\n" +
                "Nights:     %d\n" +
                "Total:      $%.2f",
                selectedCustomer.getFullName(),
                selectedRoom.getRoomNumber(), selectedRoom.getType(),
                checkIn.format(FMT), checkOut.format(FMT),
                nights, total));

            // Reset wizard
            selectedRoom     = null;
            selectedCustomer = null;
            checkIn          = null;
            checkOut         = null;
            bookedDates.clear();
            calendarMonth = YearMonth.now();
            refreshLeftPanel();
            showWizardStep1();

        } catch (ReservationConflictException ex) {
            showError("Booking conflict:\n" + ex.getMessage());
        } catch (Exception ex) {
            showError("Error: " + ex.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SMALL HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private HBox legendDot(String color, String label) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);
        Label dot = new Label("  ");
        dot.setMinSize(16, 16);
        dot.setMaxSize(16, 16);
        dot.setStyle("-fx-background-color:" + color + ";-fx-background-radius:8;");
        Label lbl = new Label(label);
        lbl.setFont(Font.font("System", 11));
        lbl.setTextFill(Color.web("#6c7086"));
        box.getChildren().addAll(dot, lbl);
        return box;
    }

    private ListCell<Customer> customerCell() {
        return new ListCell<>() {
            @Override protected void updateItem(Customer c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null : c.getFullName());
            }
        };
    }

    private TableCell<Reservation, String> statusCell() {
        return new TableCell<>() {
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
        };
    }

    private Button styledButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:" + color + ";-fx-background-radius:6;" +
                     "-fx-cursor:hand;-fx-font-size:12;-fx-padding:6 14 6 14;");
        return btn;
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }
}
