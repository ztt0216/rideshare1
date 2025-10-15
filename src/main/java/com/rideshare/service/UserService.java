package com.rideshare.service;

import com.rideshare.domain.User;
import java.math.BigDecimal;

public interface UserService {
    User register(String name, String email, String password, String role);
    User login(String email, String password);
    void updateWalletBalance(Long userId, BigDecimal amount);
    User getUserById(Long id);
}
