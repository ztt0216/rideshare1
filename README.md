# RideShare Application

## 项目概述
这是一个基于Java的打车应用系统，实现了企业级应用的架构模式和设计原则。

## 技术栈

### 后端
- **Java 17**
- **Servlet API 4.0.1**
- **PostgreSQL 17** (Render托管)
- **Jackson 2.15.2** (JSON处理)
- **Maven 3.9.11** (构建工具)
- **Tomcat 7** (应用服务器)

### 前端
- **React 18.2.0**
- **React Router 6.20.0** (路由管理)
- **Axios 1.6.2** (HTTP客户端)

## 架构设计

### 分层架构
项目采用清晰的分层架构：

1. **表现层 (Presentation Layer)** - `com.rideshare.presentation`
   - 处理HTTP请求和响应
   - Servlet控制器

2. **服务层 (Service Layer)** - `com.rideshare.service`
   - 业务逻辑处理
   - 事务协调

3. **领域层 (Domain Layer)** - `com.rideshare.domain`
   - 领域模型
   - 仓储接口
   - 工作单元模式

4. **数据源层 (Data Source Layer)** - `com.rideshare.datasource`
   - 数据库访问
   - 数据映射

### 设计模式
- **Unit of Work**: 管理业务事务
- **Repository Pattern**: 数据访问抽象
- **Domain Model**: 领域驱动设计

## 重要配置

### 时区设置
⚠️ **重要**: 系统使用墨尔本时区 (Australia/Melbourne)

所有时间相关的操作都应该使用 `TimeZoneUtil` 工具类：

```java
// 获取墨尔本当前时间
ZonedDateTime melbourneTime = TimeZoneUtil.now();

// 获取墨尔本当前本地时间
LocalDateTime melbourneLocalTime = TimeZoneUtil.nowLocal();
```

### 数据库配置
- **主机**: dpg-d3ne6rjipnbc73b1a8a0-a.oregon-postgres.render.com
- **数据库**: rideshare1
- **用户**: rideshare1_user
- **端口**: 5432

## 已实现功能

### 1. 用户管理 ✅
- 用户注册（乘客/司机）
- 用户登录
- 钱包管理（查看余额、充值）

**API端点**:
- `POST /api/users` - 注册新用户
- `POST /api/users/login` - 用户登录
- `GET /api/users/{id}` - 获取用户信息
- `POST /api/users/{id}/wallet` - 更新钱包余额

### 2. 司机班次管理 ✅
- 设置每周工作时间表
- 查询班次安排
- 更新班次
- 清除班次
- 检查司机当前是否在班

**API端点**:
- `POST /api/drivers/availability/{driverId}` - 设置/更新司机班次
- `GET /api/drivers/availability/{driverId}` - 获取司机班次
- `DELETE /api/drivers/availability/{driverId}` - 清除司机班次

### 3. 订单管理系统 ✅
- 乘客请求行程
- 司机查看可用订单
- 司机接单（乐观锁）
- 开始行程
- 完成行程（自动支付）
- 取消订单
- 行程历史记录

**费用计算规则**:
- 机场 (3045): $60
- 州际 (非3xxx): $500
- 地区 (3300-3999): $220
- 市区 (3000-3299): $40

**API端点**:
- `POST /api/rides` - 请求新订单
- `GET /api/rides` - 获取可用订单（司机）
- `GET /api/rides/{id}` - 获取订单详情
- `POST /api/rides/{id}/accept` - 接受订单
- `POST /api/rides/{id}/start` - 开始行程
- `POST /api/rides/{id}/complete` - 完成行程（触发支付）
- `POST /api/rides/{id}/cancel` - 取消订单
- `GET /api/rides/rider/{riderId}` - 乘客历史记录
- `GET /api/rides/driver/{driverId}` - 司机历史记录

**并发控制**:
- ✅ 乐观锁：防止多个司机同时接同一订单
- ✅ 悲观锁：钱包操作的事务隔离
- ✅ 状态机验证：订单状态转换控制

**支付处理**:
- ✅ 订单完成时自动处理支付
- ✅ 从乘客钱包扣款
- ✅ 向司机钱包转账
- ✅ 创建支付记录
- ✅ 原子事务保证

## 本地开发

### 前置要求
- JDK 17
- Maven 3.9.11
- PostgreSQL客户端（可选）

### 编译项目
```bash
mvn clean compile
```

### 初始化数据库
```bash
mvn exec:java -Dexec.mainClass=com.rideshare.util.DatabaseInitializer
```

### 运行应用
```bash
mvn tomcat7:run
```

应用将在 http://localhost:8080 运行

### 运行前端
```bash
# 进入前端目录
cd frontend

# 安装依赖（首次运行）
npm install

# 启动开发服务器
npm start
```

前端应用将在 http://localhost:3000 运行

详细前端使用说明请查看 [frontend/README.md](frontend/README.md)

### 运行测试
```powershell
# 测试用户管理功能
.\test-api.ps1

# 测试司机班次管理功能
.\test-driver-availability.ps1

# 测试订单管理功能
.\test-rides.ps1

# 完整系统测试（初始化数据库、启动服务器、运行所有测试）
.\test-complete.ps1
```

## 快速开始

### 完整测试流程（推荐）
```powershell
# 一键运行完整测试（包括数据库初始化、编译、启动服务器）
.\test-complete.ps1
```

详细使用说明请查看 [QUICK_START.md](QUICK_START.md)

## 数据库表结构

### users
```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    wallet_balance DECIMAL(10,2) NOT NULL DEFAULT 0.00
);
```

### driver_availability
```sql
CREATE TABLE driver_availability (
    id SERIAL PRIMARY KEY,
    driver_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    FOREIGN KEY (driver_id) REFERENCES users(id) ON DELETE CASCADE
);
```

### rides
```sql
CREATE TYPE ride_status AS ENUM ('REQUESTED', 'ACCEPTED', 'ENROUTE', 'COMPLETED', 'CANCELLED');

CREATE TABLE rides (
    id SERIAL PRIMARY KEY,
    rider_id BIGINT NOT NULL,
    driver_id BIGINT,
    pickup_location VARCHAR(500) NOT NULL,
    destination VARCHAR(500) NOT NULL,
    fare DECIMAL(10,2) NOT NULL,
    status ride_status NOT NULL DEFAULT 'REQUESTED',
    requested_time TIMESTAMP NOT NULL,
    completed_time TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (rider_id) REFERENCES users(id),
    FOREIGN KEY (driver_id) REFERENCES users(id)
);
```

### payments
```sql
CREATE TABLE payments (
    id SERIAL PRIMARY KEY,
    ride_id BIGINT NOT NULL UNIQUE,
    amount DECIMAL(10,2) NOT NULL,
    payment_time TIMESTAMP NOT NULL,
    FOREIGN KEY (ride_id) REFERENCES rides(id)
);
```

## 部署

### Render部署
项目配置了Render部署支持：
- `Procfile` - 定义启动命令
- `system.properties` - 指定Java版本

### 环境变量
确保在部署环境中设置以下环境变量：
- `DATABASE_URL` - PostgreSQL连接字符串
- `PORT` - 应用端口（Render自动设置）

## 项目结构
```
rideshare1/
├── docs/                    # 文档
│   ├── part1/              # 第一部分文档
│   ├── part2/              # 第二部分文档
│   ├── part3/              # 第三部分文档
│   └── data-samples/       # 测试数据
├── frontend/               # React前端应用
│   ├── public/            
│   ├── src/
│   │   ├── components/    # React组件
│   │   │   ├── Login.js
│   │   │   ├── Register.js
│   │   │   ├── RiderDashboard.js
│   │   │   └── DriverDashboard.js
│   │   ├── services/      # API服务
│   │   ├── App.js
│   │   └── index.js
│   ├── package.json
│   └── README.md          # 前端详细文档
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/rideshare/
│   │   │       ├── presentation/    # 控制器层
│   │   │       ├── service/        # 服务层
│   │   │       ├── domain/         # 领域层
│   │   │       ├── datasource/     # 数据源层
│   │   │       ├── util/           # 工具类
│   │   │       └── config/         # 配置类
│   │   ├── resources/
│   │   │   └── db/migration/       # 数据库迁移脚本
│   │   └── webapp/
│   │       ├── WEB-INF/
│   │       │   └── web.xml
│   │       └── index.html
│   └── test/
│       └── java/
├── pom.xml
├── Procfile
├── system.properties
└── README.md
```

## 开发团队
- 团队成员信息待添加

## 许可证
待定
