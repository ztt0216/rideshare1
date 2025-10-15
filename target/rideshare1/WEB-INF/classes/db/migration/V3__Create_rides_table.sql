-- Create enum type for ride status
CREATE TYPE ride_status AS ENUM ('REQUESTED', 'ACCEPTED', 'ENROUTE', 'COMPLETED', 'CANCELLED');

-- Create rides table
CREATE TABLE rides (
    id BIGSERIAL PRIMARY KEY,
    rider_id BIGINT NOT NULL,
    driver_id BIGINT,
    pickup_location VARCHAR(255) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    fare DECIMAL(10, 2) NOT NULL,
    status ride_status NOT NULL DEFAULT 'REQUESTED',
    requested_time TIMESTAMP NOT NULL,
    completed_time TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_rider FOREIGN KEY (rider_id) REFERENCES users(id),
    CONSTRAINT fk_driver FOREIGN KEY (driver_id) REFERENCES users(id)
);

-- Create indexes
CREATE INDEX idx_rides_rider_id ON rides(rider_id);
CREATE INDEX idx_rides_driver_id ON rides(driver_id);
CREATE INDEX idx_rides_status ON rides(status);
CREATE INDEX idx_rides_requested_time ON rides(requested_time DESC);
