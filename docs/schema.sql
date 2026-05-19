-- ================================================================
-- Hotel Management System - SQL Schema
-- Database: PostgreSQL 15+ compatible
-- ================================================================

-- ----------------------------------------------------------------
-- ROOMS
-- ----------------------------------------------------------------
CREATE TABLE rooms (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    room_number     VARCHAR(10)     NOT NULL UNIQUE,
    type            VARCHAR(10)     NOT NULL CHECK (type IN ('SINGLE', 'DOUBLE', 'SUITE')),
    price_per_night NUMERIC(10, 2)  NOT NULL CHECK (price_per_night > 0),
    status          VARCHAR(15)     NOT NULL DEFAULT 'AVAILABLE'
                                    CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'MAINTENANCE')),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  rooms                 IS 'Hotel room inventory';
COMMENT ON COLUMN rooms.id             IS 'Surrogate primary key (UUID)';
COMMENT ON COLUMN rooms.room_number    IS 'Human-readable room identifier (e.g. 101, 301A)';
COMMENT ON COLUMN rooms.type           IS 'Room category: SINGLE | DOUBLE | SUITE';
COMMENT ON COLUMN rooms.price_per_night IS 'Nightly rate in USD';
COMMENT ON COLUMN rooms.status         IS 'Operational status: AVAILABLE | OCCUPIED | MAINTENANCE';

-- ----------------------------------------------------------------
-- CUSTOMERS
-- ----------------------------------------------------------------
CREATE TABLE customers (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    phone_number    VARCHAR(20),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  customers             IS 'Registered hotel guests';
COMMENT ON COLUMN customers.email      IS 'Unique contact email; used for login / dedup';

-- ----------------------------------------------------------------
-- RESERVATIONS
-- ----------------------------------------------------------------
CREATE TABLE reservations (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id     UUID            NOT NULL,
    room_id         UUID            NOT NULL,
    check_in_date   DATE            NOT NULL,
    check_out_date  DATE            NOT NULL,
    status          VARCHAR(15)     NOT NULL DEFAULT 'CONFIRMED'
                                    CHECK (status IN ('CONFIRMED', 'CANCELLED', 'COMPLETED')),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign keys
    CONSTRAINT fk_reservation_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_reservation_room
        FOREIGN KEY (room_id) REFERENCES rooms (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    -- Business rule: check-out must be after check-in
    CONSTRAINT chk_dates
        CHECK (check_out_date > check_in_date),

    -- Prevent overlapping active reservations for the same room.
    -- The application layer also enforces this, but the DB is the last line of defence.
    -- NOTE: PostgreSQL-specific EXCLUDE constraint using btree_gist extension.
    CONSTRAINT no_overlap
        EXCLUDE USING gist (
            room_id WITH =,
            daterange(check_in_date, check_out_date, '[)') WITH &&
        )
        WHERE (status = 'CONFIRMED')
);

COMMENT ON TABLE  reservations                  IS 'Room reservation records';
COMMENT ON COLUMN reservations.check_in_date   IS 'Inclusive first night';
COMMENT ON COLUMN reservations.check_out_date  IS 'Exclusive last night (checkout day)';
COMMENT ON COLUMN reservations.status          IS 'CONFIRMED | CANCELLED | COMPLETED';

-- ----------------------------------------------------------------
-- INDEXES  (beyond the implicit PK indexes)
-- ----------------------------------------------------------------
CREATE INDEX idx_reservations_customer   ON reservations (customer_id);
CREATE INDEX idx_reservations_room       ON reservations (room_id);
CREATE INDEX idx_reservations_dates      ON reservations (check_in_date, check_out_date);
CREATE INDEX idx_reservations_status     ON reservations (status);
CREATE INDEX idx_rooms_status            ON rooms        (status);
CREATE INDEX idx_customers_email         ON customers    (email);

-- ----------------------------------------------------------------
-- SAMPLE DATA
-- ----------------------------------------------------------------
INSERT INTO rooms (room_number, type, price_per_night, status) VALUES
    ('101', 'SINGLE',  120.00, 'AVAILABLE'),
    ('102', 'SINGLE',  120.00, 'MAINTENANCE'),
    ('201', 'DOUBLE',  180.00, 'AVAILABLE'),
    ('202', 'DOUBLE',  180.00, 'AVAILABLE'),
    ('301', 'SUITE',   350.00, 'AVAILABLE');

INSERT INTO customers (first_name, last_name, email, phone_number) VALUES
    ('Alice',   'Johnson', 'alice@example.com', '+1-555-0101'),
    ('Bob',     'Smith',   'bob@example.com',   '+1-555-0102'),
    ('Charlie', 'Brown',   'charlie@example.com','+1-555-0103');
