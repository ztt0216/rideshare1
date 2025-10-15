INSERT INTO users(name,email,password_hash,role) VALUES
 ('Riley Rider','rider@example.com','password','RIDER'),
 ('Dylan Driver','driver@example.com','password','DRIVER');

INSERT INTO wallets(user_id,balance_cents,version) VALUES
 ((SELECT id FROM users WHERE email='rider@example.com'), 10000, 0),
 ((SELECT id FROM users WHERE email='driver@example.com'), 0, 0);

-- one initial REQUESTED ride for demo
INSERT INTO rides(rider_id, driver_id, pickup, dest, fare_cents, status, requested_at)
VALUES(
 (SELECT id FROM users WHERE email='rider@example.com'),
 NULL, '3000','3045', 6000, 'REQUESTED', CURRENT_TIMESTAMP
);

-- availability for driver Monday 09:00-17:00
INSERT INTO availabilities(driver_id, day_of_week, start_time, end_time)
VALUES ((SELECT id FROM users WHERE email='driver@example.com'),'MONDAY','09:00','17:00');
