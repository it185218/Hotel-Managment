# Hotel Management System

A desktop application for hotel room and reservation management, built with Java 17, JavaFX, MySQL, and Gradle.

## Overview

The system provides a graphical interface for managing the core operations of a hotel: room inventory organised by floor, customer records, and the full reservation lifecycle. All data is persisted in a MySQL database and survives application restarts.

The architecture follows Clean Architecture principles with a clear separation between domain entities, repository (data access), service (business logic), and UI layers. Reservation conflict detection is enforced at the service level — no two confirmed bookings can overlap for the same room.

## Features

- Floor management: create named floors and assign rooms to them
- Room management: add, update status (Available / Maintenance), delete, and view full booking history per room
- Customer management: register customers, view total spend and complete reservation history per customer
- Reservation wizard: select a room, choose a customer, then pick dates on a calendar that highlights already-booked days
- Conflict detection: the system prevents overlapping confirmed reservations automatically
- Dashboard: live summary of occupied rooms today, check-ins, check-outs, and total revenue
- Filters on the reservations list by guest and status

---

## Requirements

- Java 17 or later (Eclipse Temurin recommended)
- Gradle 8 or later
- MySQL Server 8 or later

---

## Setup and Installation

### 1. Install Java 17

Download and install Temurin 17 from https://adoptium.net.
During installation, enable the options to set JAVA_HOME and add Java to the PATH.

Verify the installation:
```
java -version
```

### 2. Install Gradle

Download the binary-only ZIP for Gradle 8.x from https://gradle.org/releases and extract it to `C:\Gradle\gradle-8.x`.

Add `C:\Gradle\gradle-8.x\bin` to your system PATH, then verify:
```
gradle -version
```

### 3. Install MySQL Server

Download MySQL Installer from https://dev.mysql.com/downloads/installer and run it with the Developer Default setup type.

Set a root password during installation and note it down.

### 4. Create the database

Open MySQL Workbench or the MySQL command line and run:
```sql
CREATE DATABASE IF NOT EXISTS hotel_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

### 5. Configure the database connection

Open the file:
```
src/main/resources/db.properties
```

Set your MySQL root password:
```
db.url=jdbc:mysql://localhost:3306/hotel_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
db.username=root
db.password=YOUR_PASSWORD_HERE
```

### 6. Run the application

Open a terminal in the `hotel-management` folder and run:
```
gradle run
```

On the first launch, the application creates all required database tables automatically. Subsequent launches connect directly to the existing data.

---

## Project Structure

```
hotel-management/
├── build.gradle
├── settings.gradle
└── src/main/java/com/hotel/
    ├── Main.java
    ├── domain/         Room, Customer, Reservation, Floor
    ├── enums/          RoomType, RoomStatus, ReservationStatus
    ├── repository/     Interfaces + MySQL implementations
    ├── service/        RoomService, ReservationService
    ├── exception/      NotFoundException, ReservationConflictException
    ├── db/             DatabaseConnection, DatabaseInitializer
    └── ui/             MainWindow, DashboardView, RoomsView,
                        CustomersView, ReservationsView
```
