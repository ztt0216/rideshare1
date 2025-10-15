// src/main/java/com/swen90007/rideshare/config/AppBootstrap.java
package com.swen90007.rideshare.config;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 📦 层：Application / Bootstrap（启动层）
 * 🎯 作用：应用启动钩子；根据配置初始化运行环境（如：是否建表、是否加载种子数据）。
 *
 * 🚀 AppBootstrap
 *
 * 说明：
 * - 在 WebAppContext 启动时被调用，用于准备运行所需的外部资源（数据库、表结构等）。
 * - 若你选择“只连接已有 Render PostgreSQL”，可仅做连通性检查与日志输出，不再执行 schema/seed。
 * - 失败时应抛出受检异常，中断启动，避免系统在不完整状态下运行。
 *
 * 🔧 技术点：
 * - 典型用法：实现 ServletContextListener 并在 web.xml 中注册。
 * - 可通过环境变量/系统属性读取开关：DB_INIT_ENABLED, DB_SEED_ENABLED 等。
 */



public class AppBootstrap implements ServletContextListener {
    private static final Logger log = LoggerFactory.getLogger(AppBootstrap.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // 不再做任何 DB 初始化
        log.info("AppBootstrap: DB init disabled. Using existing Render PostgreSQL.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) { }
}
