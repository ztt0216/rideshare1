package com.swen90007.rideshare.persistence.unitofwork;

import com.swen90007.rideshare.persistence.DBConnection;
import com.swen90007.rideshare.persistence.mapper.RideMapper;
import com.swen90007.rideshare.persistence.mapper.PaymentMapper;
import com.swen90007.rideshare.domain.model.Payment;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.time.*;

public class UnitOfWork {

    private static final ThreadLocal<Connection> TL = new ThreadLocal<>();

    private final Connection conn;
    private final List<Runnable> ops = new ArrayList<>();

    public UnitOfWork(Connection conn) {
        this.conn = conn;
    }

    /** 提供与 DBConnection.getConnection 相同的行为（事务性、autoCommit=false） */
    public static Connection getConnection() throws Exception {
        Connection c = TL.get();
        if (c == null || c.isClosed()) {
            c = DBConnection.getConnection();
            // DBConnection.getConnection() 内部会 setAutoCommit(false) 并绑定到 TL
            TL.set(c);
        }
        return c;
    }

    /** 把一次“修改骑行状态”的操作注册为延后执行的事务操作 */
    public void registerRideStatusUpdate(int rideId, String status) {
        ops.add(() -> {
            try {
                new RideMapper().updateStatus(conn, rideId, status);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /** 把一次“插入支付记录”的操作注册为延后执行的事务操作 */
    public void registerPaymentInsert(Payment p) {
        ops.add(() -> {
            try {
                new PaymentMapper().insert(conn, p);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /** 统一提交所有已注册操作 */
    public void commit() throws Exception {
        try {
            for (Runnable op : ops) op.run();
            conn.commit();
        } catch (Exception e) {
            try { conn.rollback(); } catch (Exception ignore) {}
            throw e;
        } finally {
            try { conn.close(); } catch (Exception ignore) {}
            TL.remove();
        }
    }
    public static final ZoneId MELBOURNE = ZoneId.of("Australia/Melbourne");

    private void Time() {}

    public static ZonedDateTime nowZoned() { return ZonedDateTime.now(MELBOURNE); }
    public static OffsetDateTime nowOffset() { return OffsetDateTime.now(MELBOURNE); }
    public static LocalDateTime nowLocal() { return LocalDateTime.now(MELBOURNE); }
    public static long nowEpochMillis() { return nowZoned().toInstant().toEpochMilli(); }

    public static LocalDateTime fromEpochMillisToLocal(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).atZone(MELBOURNE).toLocalDateTime();
    }
}
