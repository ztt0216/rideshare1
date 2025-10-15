package com.swen90007.rideshare.service;

import com.swen90007.rideshare.persistence.mapper.AvailabilityMapper;

/**
 * 司机可接单时间段 Service
 */
public class AvailabilityService {

    private final AvailabilityMapper mapper = new AvailabilityMapper();

    /**
     * 与 AvailabilityController 保持一致的签名：
     * driverId, dayOfWeek(1..7), day("MON".."SUN"), start("HH:mm"), end("HH:mm")
     */
    public void upsertAvailability(int driverId, int dayOfWeek, String day, String start, String end) {
        // 基础校验（避免脏数据）
        if (start == null || end == null || start.length() != 5 || end.length() != 5) {
            throw new IllegalArgumentException("start/end must be HH:mm");
        }
        if (day == null || day.isBlank()) {
            throw new IllegalArgumentException("day cannot be blank");
        }
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new IllegalArgumentException("dayOfWeek must be 1..7");
        }
        // 注意：允许同一天多条时间段，因此不做冲突判定，只检查 start < end
        // 具体时间比较在 Mapper 内用 LocalTime 进行
        mapper.upsert(driverId, dayOfWeek, day, start, end);
    }

    /** 供其他业务（如 RideService）查询：司机某天某时是否可接 */
    public boolean isDriverAvailable(int driverId, String day, String hhmm) {
        return mapper.isDriverAvailable(driverId, day, hhmm);
    }

    /** 返回司机所有可接单时间（实现保持你工程里已有的 JSON 输出样式） */
    public String getAvailabilityJson(int driverId) {
        return mapper.findByDriverId(driverId);
    }
}
