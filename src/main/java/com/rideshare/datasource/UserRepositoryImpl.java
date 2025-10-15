package com.rideshare.datasource;

import com.rideshare.domain.User;
import com.rideshare.domain.UserRepository;
import com.rideshare.domain.UserRole;
import com.rideshare.util.RideShareException;

import java.sql.*;
import java.util.Optional;

public class UserRepositoryImpl implements UserRepository {
    
    @Override
    public User save(User user, Connection connection) {
        String sql = "INSERT INTO users (name, email, password, role, wallet_balance) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getRole().name());
            ps.setBigDecimal(5, user.getWalletBalance());
            
            ps.executeUpdate();
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getLong(1));
                    return user;
                }
                throw new RideShareException("Failed to retrieve user ID");
            }
        } catch (SQLException e) {
            throw new RideShareException("Failed to save user", e);
        }
    }
    
    @Override
    public Optional<User> findByEmail(String email, Connection connection) {
        String sql = "SELECT * FROM users WHERE email = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RideShareException("Failed to find user by email", e);
        }
    }
    
    @Override
    public Optional<User> findById(Long id, Connection connection) {
        String sql = "SELECT * FROM users WHERE id = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RideShareException("Failed to find user by id", e);
        }
    }
    
    @Override
    public void update(User user, Connection connection) {
        String sql = "UPDATE users SET name = ?, email = ?, password = ?, role = ?, wallet_balance = ? WHERE id = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getRole().name());
            ps.setBigDecimal(5, user.getWalletBalance());
            ps.setLong(6, user.getId());
            
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RideShareException("Failed to update user", e);
        }
    }
    
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User(
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("password"),
            UserRole.valueOf(rs.getString("role"))
        );
        user.setId(rs.getLong("id"));
        user.setWalletBalance(rs.getBigDecimal("wallet_balance"));
        return user;
    }
}
