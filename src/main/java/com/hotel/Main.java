package com.hotel;

import com.hotel.domain.Customer;
import com.hotel.domain.Reservation;
import com.hotel.domain.Room;
import com.hotel.enums.RoomStatus;
import com.hotel.enums.RoomType;
import com.hotel.exception.ReservationConflictException;
import com.hotel.repository.*;
import com.hotel.service.ReservationService;
import com.hotel.service.RoomService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Entry point and demonstration driver for the Hotel Management System.
 *
 * <p>This class wires together the application's object graph (poor-man's
 * dependency injection) and then exercises the main use-cases:
 * <ol>
 *   <li>Create rooms</li>
 *   <li>Register customers</li>
 *   <li>Query available rooms for a date range</li>
 *   <li>Make a successful reservation</li>
 *   <li>Attempt a conflicting reservation (expect exception)</li>
 *   <li>Cancel a reservation</li>
 *   <li>Search reservations by customer</li>
 *   <li>Search reservations by date range</li>
 * </ol>
 */
public class Main {

    // ── ANSI colour codes ──────────────────────────────────────────────────────
    private static final String RESET  = "\u001B[0m";
    private static final String GREEN  = "\u001B[32m";
    private static final String RED    = "\u001B[31m";
    private static final String CYAN   = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BOLD   = "\u001B[1m";

    // ── Width constant ─────────────────────────────────────────────────────────
    private static final String DIVIDER =
            "═".repeat(62);

    public static void main(String[] args) {

        // ── 1. Bootstrap repositories ──────────────────────────────────────────
        RoomRepository        roomRepo        = new InMemoryRoomRepository();
        CustomerRepository    customerRepo    = new InMemoryCustomerRepository();
        ReservationRepository reservationRepo = new InMemoryReservationRepository();

        // ── 2. Bootstrap services ──────────────────────────────────────────────
        RoomService        roomService        = new RoomService(roomRepo, reservationRepo);
        ReservationService reservationService = new ReservationService(
                reservationRepo, roomRepo, customerRepo);

        header("HOTEL MANAGEMENT SYSTEM  –  Demo Runner");

        // ══════════════════════════════════════════════════════════════════════
        // STEP 1 – CREATE ROOMS
        // ══════════════════════════════════════════════════════════════════════
        section("1. Creating Rooms");

        Room r101 = roomService.createRoom("101", RoomType.SINGLE,  120.00);
        Room r201 = roomService.createRoom("201", RoomType.DOUBLE,  180.00);
        Room r301 = roomService.createRoom("301", RoomType.SUITE,   350.00);
        Room r102 = roomService.createRoom("102", RoomType.SINGLE,  120.00);

        // Put room 102 into maintenance so it shows up as unavailable
        roomService.updateRoomStatus(r102.getRoomId(), RoomStatus.MAINTENANCE);

        for (Room room : roomService.getAllRooms()) {
            printRoom(room);
        }

        // ══════════════════════════════════════════════════════════════════════
        // STEP 2 – REGISTER CUSTOMERS
        // ══════════════════════════════════════════════════════════════════════
        section("2. Registering Customers");

        Customer alice = new Customer(UUID.randomUUID(),
                "Alice", "Johnson", "alice@example.com", "+1-555-0101");
        Customer bob   = new Customer(UUID.randomUUID(),
                "Bob",   "Smith",   "bob@example.com",   "+1-555-0102");

        customerRepo.save(alice);
        customerRepo.save(bob);

        for (Customer c : customerRepo.findAll()) {
            ok("Registered: %s (%s)", c.getFullName(), c.getEmail());
        }

        // ══════════════════════════════════════════════════════════════════════
        // STEP 3 – CHECK AVAILABILITY FOR A DATE RANGE
        // ══════════════════════════════════════════════════════════════════════
        section("3. Available Rooms  (2025-06-01 → 2025-06-05)");

        LocalDate jun01 = LocalDate.of(2025, 6, 1);
        LocalDate jun05 = LocalDate.of(2025, 6, 5);

        List<Room> available = roomService.getAvailableRoomsForDateRange(jun01, jun05);
        if (available.isEmpty()) {
            warn("No rooms available for that period.");
        } else {
            available.forEach(Main::printRoom);
        }

        // ══════════════════════════════════════════════════════════════════════
        // STEP 4 – MAKE A SUCCESSFUL RESERVATION
        // ══════════════════════════════════════════════════════════════════════
        section("4. Creating Reservation — Alice in Room 101 (Jun 1–5)");

        Reservation aliceRes = reservationService.createReservation(
                alice.getCustomerId(),
                r101.getRoomId(),
                jun01, jun05);

        ok("Reservation CONFIRMED: %s in Room %s  (%s → %s)",
                alice.getFullName(),
                r101.getRoomNumber(),
                aliceRes.getCheckInDate(),
                aliceRes.getCheckOutDate());

        // ══════════════════════════════════════════════════════════════════════
        // STEP 5 – ATTEMPT A CONFLICTING RESERVATION (MUST THROW)
        // ══════════════════════════════════════════════════════════════════════
        section("5. Conflict Test — Bob tries Room 101 (Jun 3–7)");

        LocalDate jun03 = LocalDate.of(2025, 6, 3);
        LocalDate jun07 = LocalDate.of(2025, 6, 7);

        try {
            reservationService.createReservation(
                    bob.getCustomerId(),
                    r101.getRoomId(),
                    jun03, jun07);

            // Should never reach here
            fail("ERROR: Conflict was NOT detected — this is a bug!");

        } catch (ReservationConflictException ex) {
            fail("Conflict detected (expected): %s", ex.getMessage());
        }

        // ══════════════════════════════════════════════════════════════════════
        // STEP 6 – BOB MAKES A VALID RESERVATION ON A DIFFERENT ROOM
        // ══════════════════════════════════════════════════════════════════════
        section("6. Bob Books Room 201 (Jun 2–6)  — no conflict");

        LocalDate jun02 = LocalDate.of(2025, 6, 2);
        LocalDate jun06 = LocalDate.of(2025, 6, 6);

        Reservation bobRes = reservationService.createReservation(
                bob.getCustomerId(),
                r201.getRoomId(),
                jun02, jun06);

        ok("Reservation CONFIRMED: %s in Room %s  (%s → %s)",
                bob.getFullName(),
                r201.getRoomNumber(),
                bobRes.getCheckInDate(),
                bobRes.getCheckOutDate());

        // Availability query now excludes both booked rooms
        info("Available rooms for Jun 1–6 after bookings:");
        roomService.getAvailableRoomsForDateRange(jun01, jun06)
                   .forEach(Main::printRoom);

        // ══════════════════════════════════════════════════════════════════════
        // STEP 7 – CANCEL ALICE'S RESERVATION
        // ══════════════════════════════════════════════════════════════════════
        section("7. Cancelling Alice's Reservation");

        reservationService.cancelReservation(aliceRes.getReservationId());
        ok("Reservation %s cancelled.", aliceRes.getReservationId());

        // Room 101 is now available again
        info("Available rooms for Jun 1–5 after Alice's cancellation:");
        roomService.getAvailableRoomsForDateRange(jun01, jun05)
                   .forEach(Main::printRoom);

        // ══════════════════════════════════════════════════════════════════════
        // STEP 8 – SEARCH RESERVATIONS BY CUSTOMER
        // ══════════════════════════════════════════════════════════════════════
        section("8. Reservations for Alice Johnson");

        List<Reservation> aliceReservations =
                reservationService.getReservationsByCustomer(alice.getCustomerId());

        if (aliceReservations.isEmpty()) {
            warn("No reservations found for %s.", alice.getFullName());
        } else {
            aliceReservations.forEach(Main::printReservation);
        }

        // ══════════════════════════════════════════════════════════════════════
        // STEP 9 – SEARCH RESERVATIONS BY DATE RANGE
        // ══════════════════════════════════════════════════════════════════════
        section("9. All Reservations Overlapping Jun 1–10");

        LocalDate jun10 = LocalDate.of(2025, 6, 10);

        List<Reservation> inRange =
                reservationService.getReservationsByDateRange(jun01, jun10);

        if (inRange.isEmpty()) {
            warn("No reservations found in that window.");
        } else {
            inRange.forEach(Main::printReservation);
        }

        // ══════════════════════════════════════════════════════════════════════
        // STEP 10 – PRINT ALL RESERVATIONS (SUMMARY)
        // ══════════════════════════════════════════════════════════════════════
        section("10. Full Reservation Ledger");

        reservationService.getAllReservations().forEach(Main::printReservation);

        footer();
    }

    // ── Pretty-print helpers ───────────────────────────────────────────────────

    private static void printRoom(Room r) {
        System.out.printf("   %s→%s Room %-4s │ %-9s │ $%-8.2f │ %s%n",
                CYAN, RESET,
                r.getRoomNumber(),
                r.getType().name(),
                r.getPricePerNight(),
                r.getStatus().name());
    }

    private static void printReservation(Reservation r) {
        System.out.printf("   %s→%s [%-9s] %s │ Room %-4s │ %s → %s%n",
                CYAN, RESET,
                r.getStatus().name(),
                padRight(r.getCustomer().getFullName(), 18),
                r.getRoom().getRoomNumber(),
                r.getCheckInDate(),
                r.getCheckOutDate());
    }

    private static void ok(String fmt, Object... args) {
        System.out.printf("   %s✔%s  " + fmt + "%n", prepend(GREEN, args));
    }

    private static void fail(String fmt, Object... args) {
        System.out.printf("   %s✘%s  " + fmt + "%n", prepend(RED, args));
    }

    private static void warn(String fmt, Object... args) {
        System.out.printf("   %s!%s  " + fmt + "%n", prepend(YELLOW, args));
    }

    private static void info(String fmt, Object... args) {
        System.out.printf("   %s·%s  " + fmt + "%n", prepend(CYAN, args));
    }

    private static void header(String title) {
        System.out.println();
        System.out.println(BOLD + CYAN + DIVIDER + RESET);
        System.out.printf( BOLD + CYAN + "  %-60s%n" + RESET, title);
        System.out.println(BOLD + CYAN + DIVIDER + RESET);
        System.out.println();
    }

    private static void section(String title) {
        System.out.println();
        System.out.println(BOLD + YELLOW + "  ── " + title + " " + RESET);
        System.out.println();
    }

    private static void footer() {
        System.out.println();
        System.out.println(BOLD + CYAN + DIVIDER + RESET);
        System.out.println(BOLD + CYAN + "  Demo complete." + RESET);
        System.out.println(BOLD + CYAN + DIVIDER + RESET);
        System.out.println();
    }

    private static String padRight(String s, int width) {
        return String.format("%-" + width + "s", s);
    }

    /** Prepend two ANSI arguments (colour + RESET) before the user's varargs. */
    private static Object[] prepend(String colour, Object[] args) {
        Object[] result = new Object[args.length + 2];
        result[0] = colour;
        result[1] = RESET;
        System.arraycopy(args, 0, result, 2, args.length);
        return result;
    }
}
