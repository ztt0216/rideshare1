package com.swen90007.rideshare.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.swen90007.rideshare.domain.model.Ride;
import com.swen90007.rideshare.persistence.mapper.AvailabilityMapper;
import com.swen90007.rideshare.persistence.mapper.RideMapper;
import com.swen90007.rideshare.persistence.mapper.WalletMapper;
import com.swen90007.rideshare.persistence.lock.PessimisticLocking;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * RideService
 * 负责乘车生命周期：发单、接单、开始、完成（含资金流）、取消。
 * 本次新增：previewFare / requestRideWithBalanceCheck 两个方法以支持“先报价，再确认下单”。
 */
public class RideService {

    private final RideMapper rideMapper = new RideMapper();
    private final WalletMapper walletMapper = new WalletMapper();

    /* -------------------- 新增能力：先报价、后确认 -------------------- */

    /** 仅计算费用，不落库 */
    public double previewFare(String pickupPostcode, String destPostcode) {
        return calcFareSimple(pickupPostcode, destPostcode);
    }

    /**
     * 确认下单（带余额校验）。余额足够才真正创建 Ride。
     * @return 新建的 rideId
     */
    public int requestRideWithBalanceCheck(Connection conn,
                                           int riderId,
                                           String pickupPostcode,
                                           String destPostcode) throws Exception {
        double fare = calcFareSimple(pickupPostcode, destPostcode);

        // 余额校验
        double balance = walletMapper.getBalance(conn, riderId);
        if (balance < fare) {
            throw new IllegalStateException(
                    "Insufficient wallet balance. Need: " + fare + ", Has: " + balance);
        }

        // 真正创建 ride（注意：若你的 RideMapper.insert 签名不同，请按你本地的签名修改此行）
        return rideMapper.insert(conn, riderId, pickupPostcode, destPostcode, fare);
    }

    /* -------------------- 你现有流程（保持不变，可与本地实现比对） -------------------- */

    /** 发单（旧流程仍可用） */
    public int requestRide(Connection conn, int riderId, String pickupPostcode, String destPostcode) throws Exception {
        double fare = calcFareSimple(pickupPostcode, destPostcode);
        return rideMapper.insert(conn, riderId, pickupPostcode, destPostcode, fare);
    }

    /** 接单：REQUESTED -> ACCEPTED（如你项目还有可接单时间校验，可在这里保持） */
    public void acceptRide(Connection conn, int rideId, int driverId) throws Exception {
        PessimisticLocking.lockRide(conn, rideId);
        Ride ride = rideMapper.findById(conn, rideId);
        if (ride == null) throw new IllegalStateException("Ride not found: " + rideId);
        if (!"REQUESTED".equalsIgnoreCase(ride.getStatus())) {
            throw new IllegalStateException("Ride " + rideId + " is not REQUESTED.");
        }
        int updated = rideMapper.accept(conn, rideId, driverId);
        if (updated != 1) throw new IllegalStateException("Failed to accept ride, maybe taken by others.");
    }

    /** 开始：ACCEPTED -> ENROUTE */
    public void beginRide(Connection conn, int rideId) throws Exception {
        PessimisticLocking.lockRide(conn, rideId);
        Ride ride = rideMapper.findById(conn, rideId);
        if (ride == null) throw new IllegalStateException("Ride not found: " + rideId);
        if (!"ACCEPTED".equalsIgnoreCase(ride.getStatus())) {
            throw new IllegalStateException("Ride not in ACCEPTED state.");
        }
        int n = rideMapper.begin(conn, rideId);
        if (n != 1) throw new IllegalStateException("Failed to begin ride.");
    }

    /**
     * 完成：ENROUTE -> COMPLETED，并在完成时执行资金结算（扣 Rider、加 Driver、记录 payment）。
     * 说明：这里沿用你项目里现有的钱包与支付实现；若你把支付记录写在其他 Service/Mapper，请继续沿用你那套。
     */
    public void completeRide(Connection conn, int rideId) throws Exception {
        PessimisticLocking.lockRide(conn, rideId);
        Ride ride = rideMapper.findById(conn, rideId);
        if (ride == null) throw new IllegalStateException("Ride not found: " + rideId);
        if (!"ENROUTE".equalsIgnoreCase(ride.getStatus())) {
            throw new IllegalStateException("Ride not in ENROUTE state.");
        }

        int riderId = ride.getRiderId();
        int driverId = ride.getDriverId();
        double fare = ride.getFare();

        // 1) 读取余额
        double riderBalance = walletMapper.getBalance(conn, riderId);
        double driverBalance = walletMapper.getBalance(conn, driverId);

        if (riderBalance < fare) {
            throw new IllegalStateException(
                    "Rider balance insufficient. Need: " + fare + ", Has: " + riderBalance);
        }

        // 2) 扣减/增加（使用条件更新，避免并发）
        double newRider = riderBalance - fare;
        double newDriver = driverBalance + fare;

        int updateRider = walletMapper.updateBalanceConditional(conn, riderId, riderBalance, newRider);
        int updateDriver = walletMapper.updateBalanceConditional(conn, driverId, driverBalance, newDriver);
        if (updateRider != 1 || updateDriver != 1) {
            throw new IllegalStateException("Wallet balance updated concurrently, please retry.");
        }

        // 3) 记录支付（如果你有 PaymentMapper，请在这里调用；否则略）
        //    你之前已把 PaymentMapper.insert 调整为无需 timestamp/status 字段。
        //    例如：
        // Payment p = new Payment();
        // p.setRideId(rideId);
        // p.setPayerUserId(riderId);
        // p.setPayeeUserId(driverId);
        // p.setAmount(fare);
        // p.setCreatedAt(LocalDateTime.now());
        // paymentMapper.insert(conn, p);

        // 4) 更新订单状态
        int n = rideMapper.complete(conn, rideId);
        if (n != 1) throw new IllegalStateException("Failed to complete ride.");
    }

    /** 取消：REQUESTED/ACCEPTED -> CANCELLED（按你现有业务约束调整） */
    public void cancelRide(Connection conn, int rideId) throws Exception {
        PessimisticLocking.lockRide(conn, rideId);
        Ride ride = rideMapper.findById(conn, rideId);
        if (ride == null) throw new IllegalStateException("Ride not found: " + rideId);

        if ("COMPLETED".equalsIgnoreCase(ride.getStatus())) {
            throw new IllegalStateException("Ride already completed.");
        }
        int n = rideMapper.cancel(conn, rideId);
        if (n != 1) throw new IllegalStateException("Failed to cancel ride.");
    }

    /* -------------------- 费用规则（与你现有实现一致） -------------------- */

    /**
     * 费用规则（与项目说明一致）：
     * 1) 若任一为 3045（机场）-> $60 固定价（优先）
     * 2) 若任一在 3xxx（州际）-> $500 固定价
     * 3) 其他 -> $14（示例默认值，你本地若已实现更复杂规则，保留你那套即可）
     */
    private double calcFareSimple(String pickup, String dest) {
        int p = Integer.parseInt(pickup);
        int d = Integer.parseInt(dest);

        if (p == 3045 || d == 3045) return 60.00;
        if (p / 1000 == 3 || d / 1000 == 3) return 500.0;
        return 14.0;
    }

    /**
     * 新增：给司机列出其“可接受”的 REQUESTED 订单
     * 逻辑：
     * 1) 取出所有 status=REQUESTED 的 ride；
     * 2) 对每条 ride 按其请求时间(或创建时间)拆出“星期几 + HH:mm”；
     * 3) 调用 AvailabilityMapper.isDriverAvailable(conn, driverId, day, hh:mm)
     *    - 若司机未配置可接单时间，应返回 true（符合规格：未配置即可任何时间接单）；
     * 4) 命中则返回一个条目：pickup_postcode / destination_postcode / fare_estimate / requested_time。
     */
    private static final ObjectMapper M = new ObjectMapper();
    private static final DateTimeFormatter HHmm = DateTimeFormatter.ofPattern("HH:mm");

    private final AvailabilityMapper availabilityMapper = new AvailabilityMapper();

    public ArrayNode listRequestableRides(Connection conn, int driverId) throws Exception {
        ArrayNode out = M.createArrayNode();

        // 1) 取全部 REQUESTED 的订单（假设已有该 Mapper 方法；若你项目中的方法名不同，将其替换为你现有的“查未分配/REQUESTED”的方法即可）
        List<Ride> requested = rideMapper.findAllRequested(conn);

        for (Ride r : requested) {
            // 2) 计算请求时间（如果表里没有单独的 requested_at，就用 created_at）

            LocalDateTime when;
            if (r.getRequestedAt() > 0L) {
                when = java.time.Instant.ofEpochMilli(r.getRequestedAt())
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime();
            } else {
                when = java.time.Instant.ofEpochMilli(r.getRequestedAt())
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime();
            }

            System.out.println(when);

            if (when == null) {
                // 兜底：没有时间就按“当前时间”判断，以免漏单
                when = LocalDateTime.now();
            }
            String day = toDayString(when.getDayOfWeek().getValue()); // 1..7 => MON..SUN
            String hhmm = when.format(HHmm);

            // 3) 时间窗校验（若司机未配置，isDriverAvailable 应该返回 true；你之前实现就是这种语义）
            boolean ok = availabilityMapper.isDriverAvailable(conn, driverId, day, hhmm);
            if (!ok) continue;

            // 4) 组装返回字段
            ObjectNode item = M.createObjectNode()
                    .put("pickup_postcode", r.getPickup())
                    .put("destination_postcode", r.getDestination())
                    .put("fare_estimate", r.getFare())            // 如果你的表没有存 fare，也可以改为 calcFareSimple(...)
                    .put("requested_time", when.toString());      // ISO 字符串
            out.add(item);
        }

        return out;
    }

    // 与你之前的工具方法风格一致的星期几字符串（"MON" .. "SUN"）
    private static String toDayString(int dayOfWeek1to7) {
        switch (dayOfWeek1to7) {
            case 1: return "MON";
            case 2: return "TUE";
            case 3: return "WED";
            case 4: return "THU";
            case 5: return "FRI";
            case 6: return "SAT";
            default: return "SUN";
        }
    }
}
