CREATE TABLE availability_slots (
    id          BIGSERIAL PRIMARY KEY,
    type        VARCHAR(20) NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    start_time  TIME        NOT NULL,
    end_time    TIME        NOT NULL,
    reason      VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_availability_slots_type CHECK (type IN ('WORKING_RANGE', 'BREAK')),
    CONSTRAINT ck_availability_slots_day CHECK (day_of_week IN (
        'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    CONSTRAINT ck_availability_slots_time_order CHECK (end_time > start_time)
);

CREATE INDEX idx_availability_slots_day_type ON availability_slots (day_of_week, type);

CREATE TABLE blocked_dates (
    id         BIGSERIAL PRIMARY KEY,
    start_date DATE        NOT NULL,
    end_date   DATE        NOT NULL,
    start_time TIME,
    end_time   TIME,
    reason     VARCHAR(255),
    source     VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_blocked_dates_source CHECK (source IN ('MANUAL', 'HOLIDAY_API')),
    CONSTRAINT ck_blocked_dates_date_order CHECK (end_date >= start_date),
    CONSTRAINT ck_blocked_dates_time_pair CHECK ((start_time IS NULL) = (end_time IS NULL)),
    CONSTRAINT ck_blocked_dates_time_order CHECK (start_time IS NULL OR end_time > start_time),
    CONSTRAINT ck_blocked_dates_partial_single_day CHECK (start_time IS NULL OR start_date = end_date)
);

CREATE INDEX idx_blocked_dates_range ON blocked_dates (start_date, end_date);

-- Holiday sync idempotency
CREATE UNIQUE INDEX uq_blocked_dates_holiday_date
    ON blocked_dates (start_date) WHERE source = 'HOLIDAY_API';
