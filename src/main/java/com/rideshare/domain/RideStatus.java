package com.rideshare.domain;

public enum RideStatus {
    REQUESTED,   // 已请求
    ACCEPTED,    // 已接单
    ENROUTE,     // 进行中
    COMPLETED,   // 已完成
    CANCELLED    // 已取消
}
