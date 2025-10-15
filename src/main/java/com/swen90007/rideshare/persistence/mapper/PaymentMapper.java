package com.swen90007.rideshare.persistence.mapper;

import com.swen90007.rideshare.domain.model.Payment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class PaymentMapper {

    /** 插入一条支付记录 */
    public int insert(Connection conn, Payment p) throws SQLException {
        final String sql =
                "INSERT INTO payment (ride_id, payer_user_id, payee_user_id, amount, created_at, timestamp) " +
                        "VALUES (?, ?, ?, ?, timezone('Australia/Melbourne', now()), ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p.getRideId());
            ps.setInt(2, p.getPayerUserId());
            ps.setInt(3, p.getPayeeUserId());
            ps.setDouble(4, p.getAmount());
            // ✅ 直接在这里生成时间戳（毫秒数）
            long ts = System.currentTimeMillis();
            ps.setLong(5, ts);

            return ps.executeUpdate();
        }
    }
}
