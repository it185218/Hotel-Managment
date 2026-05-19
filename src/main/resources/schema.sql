-- Hotel Management System - MySQL Schema
-- Run this once to initialise the database

CREATE DATABASE IF NOT EXISTS hotel_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE hotel_db;

CREATE TABLE IF NOT EXISTS rooms (
    id            CHAR(36)       NOT NULL PRIMARY KEY,
    room_number   VARCHAR(10)    NOT NULL UNIQUE,
    type          VARCHAR(10)    NOT NULL,
    price_per_night DECIMAL(10,2) NOT NULL,
    status        VARCHAR(15)    NOT NULL DEFAULT 'AVAILABLE',
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS customers (
    id            CHAR(36)       NOT NULL PRIMARY KEY,
    first_name    VARCHAR(100)   NOT NULL,
    last_name     VARCHAR(100)   NOT NULL,
    email         VARCHAR(255)   NOT NULL UNIQUE,
    phone_number  VARCHAR(20),
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reservations (
    id              CHAR(36)     NOT NULL PRIMARY KEY,
    customer_id     CHAR(36)     NOT NULL,
    room_id         CHAR(36)     NOT NULL,
    check_in_date   DATE         NOT NULL,
    check_out_date  DATE         NOT NULL,
    status          VARCHAR(15)  NOT NULL DEFAULT 'CONFIRMED',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_customer  FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_room      FOREIGN KEY (room_id)     REFERENCES rooms(id),
    CONSTRAINT chk_dates    CHECK (check_out_date > check_in_date)
);
