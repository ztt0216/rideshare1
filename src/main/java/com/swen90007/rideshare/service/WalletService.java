package com.swen90007.rideshare.service;

import com.swen90007.rideshare.domain.model.Wallet;
import com.swen90007.rideshare.persistence.DBConnection;
import com.swen90007.rideshare.persistence.mapper.WalletMapper;

import java.sql.Connection;

/**
 * Wallet 领域服务：
 * - get(userId): 获取或创建钱包
 * - topUp(userId, amount): 充值（无返回值）
 * - topUpAndReturn(userId, amount): 充值并返回更新后的 Wallet 对象
 */
public class WalletService {

    private final WalletMapper walletMapper = new WalletMapper();

    /**
     * 获取用户钱包（若不存在则创建），返回模型。
     */
    public Wallet get(int userId) throws Exception {
        Connection conn = DBConnection.openConnection();
        try {
            walletMapper.insertIfAbsent(conn, userId);
            double balance = walletMapper.getBalance(conn, userId);
            conn.commit();
            return new Wallet(userId, balance);
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.close();
        }
    }

    /**
     * 充值（正数），不返回对象。
     */
    public void topUp(int userId, double amount) throws Exception {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        Connection conn = DBConnection.openConnection();
        try {
            walletMapper.insertIfAbsent(conn, userId);
            double balance = walletMapper.getBalance(conn, userId);
            double newBal = balance + amount;
            int ok = walletMapper.updateBalanceConditional(conn, userId, balance, newBal);
            if (ok != 1) {
                throw new IllegalStateException("Wallet updated concurrently, please retry.");
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.close();
        }
    }

    /**
     * 充值（正数），返回更新后的 Wallet 对象。
     */
    public Wallet topUpAndReturn(int userId, double amount) throws Exception {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        Connection conn = DBConnection.openConnection();
        try {
            walletMapper.insertIfAbsent(conn, userId);
            double balance = walletMapper.getBalance(conn, userId);
            double newBal = balance + amount;
            int ok = walletMapper.updateBalanceConditional(conn, userId, balance, newBal);
            if (ok != 1) {
                throw new IllegalStateException("Wallet updated concurrently, please retry.");
            }
            conn.commit();
            return new Wallet(userId, newBal); // ✅ 返回更新后的钱包对象
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.close();
        }
    }
}
