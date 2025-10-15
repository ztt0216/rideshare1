DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS rides;
DROP TABLE IF EXISTS wallets;
DROP TABLE IF EXISTS availabilities;
DROP TABLE IF EXISTS users;

CREATE TABLE users(
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(200) UNIQUE NOT NULL,
  password_hash VARCHAR(200) NOT NULL,
  role VARCHAR(20) NOT NULL CHECK (role IN ('RIDER','DRIVER'))
);

CREATE TABLE wallets(
  user_id BIGINT PRIMARY KEY,
  balance_cents BIGINT NOT NULL DEFAULT 0,
  version INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_wallet_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE rides(
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  rider_id BIGINT NOT NULL,
  driver_id BIGINT,
  pickup VARCHAR(10) NOT NULL,
  dest VARCHAR(10) NOT NULL,
  fare_cents BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL CHECK (status IN ('REQUESTED','ACCEPTED','ENROUTE','COMPLETED','CANCELLED')),
  requested_at TIMESTAMP NOT NULL,
  accepted_at TIMESTAMP,
  completed_at TIMESTAMP,
  CONSTRAINT fk_rides_rider FOREIGN KEY(rider_id) REFERENCES users(id),
  CONSTRAINT fk_rides_driver FOREIGN KEY(driver_id) REFERENCES users(id)
);

CREATE TABLE payments(
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  ride_id BIGINT UNIQUE NOT NULL,
  amount_cents BIGINT NOT NULL,
  ts TIMESTAMP NOT NULL,
  CONSTRAINT fk_payments_ride FOREIGN KEY(ride_id) REFERENCES rides(id)
);

CREATE TABLE availabilities(
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  driver_id BIGINT NOT NULL,
  day_of_week VARCHAR(10) NOT NULL,
  start_time VARCHAR(5) NOT NULL,
  end_time VARCHAR(5) NOT NULL,
  CONSTRAINT fk_av_driver FOREIGN KEY(driver_id) REFERENCES users(id)
);

CREATE INDEX idx_rides_status ON rides(status);
