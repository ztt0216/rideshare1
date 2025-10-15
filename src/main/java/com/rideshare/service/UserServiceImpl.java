package com.rideshare.service;

import com.rideshare.domain.User;
import com.rideshare.domain.UserRepository;
import com.rideshare.domain.UserRole;
import com.rideshare.domain.unitofwork.UnitOfWork;
import com.rideshare.domain.unitofwork.DatabaseUnitOfWork;
import com.rideshare.datasource.UserRepositoryImpl;
import com.rideshare.util.RideShareException;

import java.math.BigDecimal;

public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    public UserServiceImpl() {
        this.userRepository = new UserRepositoryImpl();
    }

    @Override
    public User register(String name, String email, String password, String role) {
        UnitOfWork uow = new DatabaseUnitOfWork();
        try {
            uow.begin();
            
            // Check if email already exists
            if (userRepository.findByEmail(email, uow.getConnection()).isPresent()) {
                throw new RideShareException("Email already registered");
            }

            // Create new user
            User user = new User(name, email, password, UserRole.valueOf(role.toUpperCase()));
            
            // Save user
            User savedUser = userRepository.save(user, uow.getConnection());
            
            uow.commit();
            return savedUser;
        } catch (Exception e) {
            uow.rollback();
            throw new RideShareException("Failed to register user", e);
        }
    }

    @Override
    public User login(String email, String password) {
        UnitOfWork uow = new DatabaseUnitOfWork();
        try {
            uow.begin();
            
            User user = userRepository.findByEmail(email, uow.getConnection())
                .orElseThrow(() -> new RideShareException("User not found"));

            if (!user.getPassword().equals(password)) {
                throw new RideShareException("Invalid password");
            }

            uow.commit();
            return user;
        } catch (Exception e) {
            uow.rollback();
            throw new RideShareException("Failed to login", e);
        }
    }

    @Override
    public void updateWalletBalance(Long userId, BigDecimal amount) {
        UnitOfWork uow = new DatabaseUnitOfWork();
        try {
            uow.begin();
            
            User user = userRepository.findById(userId, uow.getConnection())
                .orElseThrow(() -> new RideShareException("User not found"));

            user.setWalletBalance(user.getWalletBalance().add(amount));
            userRepository.update(user, uow.getConnection());

            uow.commit();
        } catch (Exception e) {
            uow.rollback();
            throw new RideShareException("Failed to update wallet balance", e);
        }
    }

    @Override
    public User getUserById(Long id) {
        UnitOfWork uow = new DatabaseUnitOfWork();
        try {
            uow.begin();
            
            User user = userRepository.findById(id, uow.getConnection())
                .orElseThrow(() -> new RideShareException("User not found"));

            uow.commit();
            return user;
        } catch (Exception e) {
            uow.rollback();
            throw new RideShareException("Failed to get user", e);
        }
    }
}
