CREATE TABLE driver_availability (
    id SERIAL PRIMARY KEY,
    driver_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    FOREIGN KEY (driver_id) REFERENCES users(id) ON DELETE CASCADE
);
