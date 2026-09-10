CREATE TABLE recurrence_groups (
    id          BIGSERIAL PRIMARY KEY,
    patient_id  BIGINT      NOT NULL REFERENCES users (id),
    day_of_week VARCHAR(10) NOT NULL,
    start_time  TIME        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_recurrence_groups_day CHECK (day_of_week IN (
        'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'))
);

CREATE TABLE appointments (
    id                  BIGSERIAL PRIMARY KEY,
    patient_id          BIGINT      NOT NULL REFERENCES users (id),
    recurrence_group_id BIGINT      REFERENCES recurrence_groups (id),
    date                DATE        NOT NULL,
    start_time          TIME        NOT NULL,
    end_time            TIME        NOT NULL,
    status              VARCHAR(25) NOT NULL,
    reminder_sent       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_appointments_status CHECK (status IN (
        'SOLICITADO', 'PENDIENTE_COMPROBANTE', 'PENDIENTE_APROBACION',
        'CONFIRMADO', 'RECHAZADO', 'CANCELADO', 'FINALIZADO')),
    CONSTRAINT ck_appointments_time_order CHECK (end_time > start_time)
);

-- Only CONFIRMADO occupies the slot
CREATE UNIQUE INDEX uq_appointments_confirmed_slot
    ON appointments (date, start_time) WHERE status = 'CONFIRMADO';

-- One live appointment per patient per day
CREATE UNIQUE INDEX uq_appointments_patient_live_day
    ON appointments (patient_id, date) WHERE status IN (
        'SOLICITADO', 'PENDIENTE_COMPROBANTE', 'PENDIENTE_APROBACION', 'CONFIRMADO');

CREATE INDEX idx_appointments_patient_date ON appointments (patient_id, date);
