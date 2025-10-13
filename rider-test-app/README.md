# RideShare Reference Implementation (SWEN90007)

This project provides a layered Java reference implementation that fulfils the architectural requirements outlined in the SWEN90007 2025 project brief. It demonstrates a clean separation between presentation, service, domain model, data mapper, and data source layers together with a lightweight Unit of Work implementation, concurrency-safe ride lifecycle handling, and a minimal HTTP surface for front-end testing.

## Project Layout

```
src/com/unimelb/rideshare
├── App.java                         # Entry point (boots HTTP server, optional console UI)
├── ApplicationContext.java          # Central wiring of services/mappers/data source
├── common/                         # Shared primitives (e.g., Result)
├── concurrency/                    # Fine-grained locking utilities
├── datasource/                     # Data source abstraction and in-memory implementation
├── domain/
│   ├── model/                      # Entities (Driver, Rider, Ride, RideRequest, …)
│   └── value/                      # Value objects (Location, Vehicle, AvailabilityWindow, …)
├── mapper/                         # Data mapper pattern implementations & registry
├── presentation/                   # Console UI & lightweight HTTP server
├── service/                        # Service layer contracts
├── service/impl/                   # Service implementations & matching strategy
└── unitofwork/                     # Unit of Work factory + implementation
```

`web/index.html` contains a static front-end panel that exercises the exposed HTTP endpoints.

## Building & Running

```powershell
# Compile
javac -d bin (Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName })

# Start the HTTP API (runs on http://localhost:8080)
java -cp bin com.unimelb.rideshare.App

# 或者同时启用控制台调试界面
java -cp bin com.unimelb.rideshare.App --console
```

运行之后：

1. 打开 `web/index.html`（双击或拖入浏览器）即可使用 JS 面板调用接口测试流程，例如注册司机/乘客、创建请求、匹配司机、执行生命周期操作等。
2. 如需命令行体验，可添加 `--console` 参数启动原有控制台菜单，与 HTTP 服务并行运行。

## 可用 API 概览

所有接口均为 `http://localhost:8080/api/...`，采用 `application/x-www-form-urlencoded` 形式提交：

- `POST /drivers/register` – 注册司机
- `POST /drivers/availability` – 更新司机班次
- `POST /riders/register` – 注册乘客
- `POST /rides/request` – 创建行程请求
- `POST /rides/match` – 为请求匹配司机
- `POST /rides/accept | /start | /complete | /cancel` – 行程状态流转
- `GET /drivers`、`GET /rides/open`、`GET /rides/active` – 查询基础数据

前端页面中的各表单已封装上述调用，方便快速验证。

## Architectural Highlights

- **Presentation layer** – `WebServer` 暴露 RESTful 风格接口，`ConsoleApplication` 提供命令行交互，两者均仅调用服务层。
- **Service layer** – `DriverServiceImpl`、`RiderServiceImpl`、`RideServiceImpl` 封装业务规则并通过 Unit of Work 与数据映射器交互。
- **Domain model** – 实体捕获状态转换（如 `RideRequest`、`Ride`）并校验业务不变量，值对象封装无副作用类型。
- **Data source layer** – `InMemoryDataStore` 隐藏持久化细节，可随时替换为 JDBC/JPA/云数据库以满足 Render 部署要求。
- **Data mapper pattern** – 各聚合拥有独立的 mapper（例如 `DriverDataMapper`、`RideDataMapper`），实现对象-文档转换。
- **Unit of Work** – `UnitOfWork` 批量提交新增/更新/删除，保证聚合一致性。
- **Concurrency guards** – `LockManager` 在行程接受阶段串行化关键流程，`DriverAvailabilityGuard` 对司机班次读写加锁，避免匹配与班次更新互相干扰。

## Next Steps

当前实现仍使用内存数据源和本地 HTTP 服务，方便快速验证。要面向 Render 全量部署，需要：

1. 接入托管数据库并替换 `InMemoryDataStore`。
2. 将 HTTP 层改造成正式 Web Service（可使用 Spring Boot/Spark 等框架）。
3. 将静态前端托管到 Render（或其它 CDN），并配置后端服务与数据库的环境变量/网络。
4. 加入鉴权、异常监控、集成测试等生产级能力。