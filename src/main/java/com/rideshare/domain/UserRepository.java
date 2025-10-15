package com.rideshare.domain;

import java.sql.Connection;
import java.util.Optional;

public interface UserRepository {
    User save(User user, Connection connection);
    Optional<User> findByEmail(String email, Connection connection);
    Optional<User> findById(Long id, Connection connection);
    void update(User user, Connection connection);
}
