# 🏨 Hotel Management System

A production-style, console-based Hotel Management System built with **Java 17**, **Gradle (Groovy DSL)**, and **Clean Architecture** principles. Designed as both an academic and production-quality OOP showcase.

---

## 📋 Project Description

This system simulates a real hotel management workflow:
- Room inventory management (SINGLE, DOUBLE, SUITE)
- Customer registration
- Reservation lifecycle (create, confirm, cancel, complete)
- Conflict detection (overlapping date validation)
- Search and reporting via Java Streams

---

## 🚀 How to Run

### Prerequisites
- Java 17+ installed
- Gradle 8+ installed (or use `./gradlew`)

### Build
```bash
cd hotel-management
gradle build
```

### Run
```bash
gradle run
```

---

## ✅ Features

| Feature | Description |
|---|---|
| Room Management | Create rooms with type, price, and status |
| Availability Check | Query available rooms for a date range |
| Reservation Creation | Book a room with full conflict validation |
| Conflict Detection | Prevents overlapping reservations |
| Cancel Reservation | Change reservation status to CANCELLED |
| Search by Customer | Filter reservations by customer ID |
| Search by Date Range | Filter reservations overlapping a date window |
| Stream-based Queries | All searches use Java Streams |
| Exception Handling | Custom exceptions for business rule violations |

---

## 🏗️ Architecture

```
com.hotel/
├── domain/         → Pure entities (Room, Customer, Reservation)
├── enums/          → RoomType, RoomStatus, ReservationStatus
├── repository/     → In-memory data access (interface + impl)
├── service/        → Business logic (RoomService, ReservationService)
├── exception/      → Domain exceptions
└── Main.java       → Demo runner
```

- **Clean Architecture**: Domain → Repository → Service → Main
- **SOLID**: Each class has a single responsibility; services depend on repository abstractions
- **No external dependencies**: Pure Java stdlib

---

## 📊 Example Console Output

```
============================================================
 HOTEL MANAGEMENT SYSTEM - Demo
============================================================

[ROOMS] Creating rooms...
  ✔ Created: Room 101 | SINGLE    | $120.00/night | AVAILABLE
  ✔ Created: Room 201 | DOUBLE    | $180.00/night | AVAILABLE
  ✔ Created: Room 301 | SUITE     | $350.00/night | AVAILABLE
  ✔ Created: Room 102 | SINGLE    | $120.00/night | MAINTENANCE

[CUSTOMERS] Registering customers...
  ✔ Registered: Alice Johnson (alice@example.com)
  ✔ Registered: Bob Smith (bob@example.com)

[AVAILABILITY] Rooms available from 2025-06-01 to 2025-06-05:
  → Room 101 | SINGLE    | $120.00/night
  → Room 201 | DOUBLE    | $180.00/night
  → Room 301 | SUITE     | $350.00/night

[RESERVATION] Making reservation for Alice (Room 101, Jun 1-5)...
  ✔ Reservation CONFIRMED: Alice Johnson in Room 101 (2025-06-01 → 2025-06-05)

[CONFLICT TEST] Trying conflicting reservation (Room 101, Jun 3-7)...
  ✘ Conflict detected: Room 101 is already booked from 2025-06-01 to 2025-06-05

[CANCEL] Cancelling Alice's reservation...
  ✔ Reservation cancelled successfully.

[SEARCH] All reservations for Alice Johnson:
  → [CANCELLED] Room 101 | 2025-06-01 → 2025-06-05

[SEARCH] Reservations in date range Jun 1-10:
  → [CONFIRMED] Bob Smith | Room 201 | 2025-06-02 → 2025-06-06

============================================================
 Demo complete.
============================================================
```

---

## 🗺️ UML & SQL

See `docs/uml.puml` and `docs/schema.sql` for the full class diagram and database schema.

---

## 📄 License

Academic/demo project. Free to use and adapt.
