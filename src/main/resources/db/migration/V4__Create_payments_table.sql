-- Create payments table
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    ride_id BIGINT NOT NULL UNIQUE,
    rider_id BIGINT NOT NULL,
    driver_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_time TIMESTAMP NOT NULL,
    CONSTRAINT fk_payment_ride FOREIGN KEY (ride_id) REFERENCES rides(id),
    CONSTRAINT fk_payment_rider FOREIGN KEY (rider_id) REFERENCES users(id),
    CONSTRAINT fk_payment_driver FOREIGN KEY (driver_id) REFERENCES users(id)
);

-- Create indexes
CREATE INDEX idx_payments_ride_id ON payments(ride_id);
CREATE INDEX idx_payments_rider_id ON payments(rider_id);
CREATE INDEX idx_payments_driver_id ON payments(driver_id);
CREATE INDEX idx_payments_payment_time ON payments(payment_time DESC);
