package com.swen90007.rideshare.service;

import com.swen90007.rideshare.domain.model.User;
import com.swen90007.rideshare.persistence.DBConnection;
import com.swen90007.rideshare.persistence.mapper.UserMapper;
import com.swen90007.rideshare.persistence.mapper.WalletMapper;

import java.sql.Connection;

/**
 * 用户相关应用服务（Jakarta 版）
 * 显式事务控制，防止注册成功但数据库未写入。
 */
public class UserService {

    private final UserMapper userMapper = new UserMapper();
    private final WalletMapper walletMapper = new WalletMapper();

    public User register(User user) throws Exception {
        Connection conn = DBConnection.getConnection();
        boolean previousAutoCommit = true;

        try {
            previousAutoCommit = conn.getAutoCommit();
            if (previousAutoCommit) {
                conn.setAutoCommit(false);
            }

            // 插入用户
            int newUserId = userMapper.insert(conn, user);
            user.setId(newUserId);

            // 插入钱包（如果没有则创建）
            walletMapper.insertIfAbsent(conn, newUserId);

            // 提交事务
            conn.commit();
            return user;

        } catch (Exception e) {
            try { conn.rollback(); } catch (Exception ignore) {}
            throw e;
        } finally {
            try { conn.setAutoCommit(previousAutoCommit); } catch (Exception ignore) {}
            DBConnection.closeThreadConnection();
        }
    }

    public User findByEmail(String email) throws Exception {
        Connection conn = DBConnection.getConnection();
        try {
            return userMapper.findByEmail(conn, email);
        } finally {
            DBConnection.closeThreadConnection();
        }
    }

    public User findById(int id) throws Exception {
        Connection conn = DBConnection.getConnection();
        try {
            return userMapper.findById(conn, id);
        } finally {
            DBConnection.closeThreadConnection();
        }
    }
}
