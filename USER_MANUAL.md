# 🚗 Rideshare System - User Manual

## 快速启动指南

### 启动前端
```powershell
cd d:\learning\2025sm2\SWEN90007\rideshare1\frontend-new
npm run dev
```
访问: **http://localhost:3000**

### 后端地址
已部署在 Render: **https://rideshare-backend1.onrender.com**

---

## 📱 功能演示流程

### 场景 1: 乘客完整流程 🙋‍♂️

#### 步骤 1: 注册乘客账号
1. 打开 http://localhost:3000
2. 点击 **"Register"** 按钮
3. 填写表单:
   - Name: `Alice`
   - Email: `alice@example.com`
   - Password: `password123`
4. 点击 **"Register"**
5. ✅ 注册成功后自动跳转到登录页面

#### 步骤 2: 登录
1. 使用刚才的邮箱和密码登录
2. ✅ 登录成功后进入 **Rider Dashboard**

#### 步骤 3: 充值钱包
1. 在 **My Wallet** 区域，看到当前余额: `$0`
2. 在输入框中输入金额: `100`
3. 点击 **"💰 Add Money"** 按钮
4. ✅ 余额更新为 `$70` (已经充值过了)

#### 步骤 4: 请求行程
1. 在 **Create Ride** 区域:
   - Pickup Location: `3000`
   - Destination: `3045` (机场，票价 $60)
2. 点击 **"Confirm Request"** 按钮
3. ✅ 成功创建行程！显示 "Ride created successfully!"
4. 页面刷新，可以在行程列表中看到新创建的行程

#### 步骤 5: 查看行程状态
在 **My Rides** 区域可以看到:
- 状态: `REQUESTED` (等待司机接单)
- 从: `3000`
- 到: `3045`
- 票价: `$60`

---

### 场景 2: 司机完整流程 👨‍✈️

#### 步骤 1: 注册司机账号
1. **退出登录** (如果还在乘客账号中)
2. 点击 **"Register"**
3. 填写表单:
   - Name: `Bob Driver`
   - Email: `bob@driver.com`
   - Password: `driver123`
   - **Role**: 选择 `Driver` (重要!)
4. 点击 **"Register"**
5. ✅ 注册成功

#### 步骤 2: 登录司机账号
1. 使用司机邮箱和密码登录
2. ✅ 登录成功后进入 **Driver Dashboard**

#### 步骤 3: 查看可接单行程
在 **Available Rides** 区域可以看到:
- 刚才 Alice 创建的行程
- 显示: 从 `3000` 到 `3045`, 票价 `$60`
- 状态: `REQUESTED`

#### 步骤 4: 接单
1. 找到 Alice 的行程
2. 点击 **"Accept"** 按钮
3. ✅ 接单成功！行程从列表中消失（因为已被接受）

#### 步骤 5: 开始行程
1. 在 **My Rides** 区域找到刚接的行程
2. 状态应该是 `ACCEPTED`
3. 点击 **"Start Ride"** 按钮
4. ✅ 行程开始！状态变为 `ENROUTE`

#### 步骤 6: 完成行程
1. 在同一行程上
2. 点击 **"Complete"** 按钮
3. ✅ 行程完成！
4. **自动触发支付**:
   - Alice 钱包扣除 $60
   - Bob 钱包增加 $60
5. 状态变为 `COMPLETED`

#### 步骤 7: 查看收入
在 **My Wallet** 区域:
- 余额应该显示 `$60` (之前是 $0)
- 可以看到刚才完成的行程

---

### 场景 3: 取消行程 ❌

#### 作为乘客取消行程:
1. 登录乘客账号
2. 创建一个新行程
3. 在 **My Rides** 列表中找到该行程
4. 如果状态是 `REQUESTED` (还没有司机接单)
5. 点击 **"Cancel"** 按钮
6. ✅ 行程取消成功！状态变为 `CANCELLED`

**注意**: 只能取消状态为 `REQUESTED` 的行程

---

## 💰 票价参考

根据目的地邮编计算票价:

| 目的地 | 邮编范围 | 票价 |
|--------|----------|------|
| **市区** | 3000-3299 | $40 |
| **机场** | 3045 | $60 |
| **地区** | 3300-3999 | $220 |
| **州际** | 其他 | $500 |

### 常用测试邮编:
- `3000`: 墨尔本市中心
- `3045`: 墨尔本机场
- `3100`: Carlton (市区)
- `3500`: 地区城镇
- `4000`: 其他州

---

## 🎨 界面说明

### Rider Dashboard (乘客界面)
```
┌─────────────────────────────────────────┐
│         Rider Dashboard                 │
├─────────────────────────────────────────┤
│                                         │
│  My Wallet                Ride Service  │
│  ┌────────┐               ┌──────────┐ │
│  │  $70   │               │ Fare Info│ │
│  │        │               │ • City   │ │
│  │ [Add   │               │ • Airport│ │
│  │  Money]│               │ [Cancel] │ │
│  └────────┘               └──────────┘ │
│                                         │
│  Create Ride                            │
│  ┌─────────────────────────────────┐   │
│  │ Pickup: [3000           ]       │   │
│  │ Dest:   [3045           ]       │   │
│  │        [Confirm Request]        │   │
│  └─────────────────────────────────┘   │
│                                         │
│  My Rides                               │
│  ┌─────────────────────────────────┐   │
│  │ Ride #1: 3000 → 3045            │   │
│  │ Status: REQUESTED | Fare: $60   │   │
│  │              [Cancel]           │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

### Driver Dashboard (司机界面)
```
┌─────────────────────────────────────────┐
│         Driver Dashboard                │
├─────────────────────────────────────────┤
│                                         │
│  My Wallet                              │
│  ┌────────┐                             │
│  │  $60   │                             │
│  └────────┘                             │
│                                         │
│  Available Rides                        │
│  ┌─────────────────────────────────┐   │
│  │ Ride #2: 3000 → 3045            │   │
│  │ Fare: $60      [Accept]         │   │
│  └─────────────────────────────────┘   │
│                                         │
│  My Rides                               │
│  ┌─────────────────────────────────┐   │
│  │ Ride #1: 3000 → 3045            │   │
│  │ Status: ACCEPTED | Fare: $60    │   │
│  │        [Start] [Complete]       │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

---

## 🐛 常见问题

### Q1: 注册失败，提示 "Failed to register user"
**解决**: 
- 检查邮箱是否已经被使用
- 尝试使用不同的邮箱地址

### Q2: 创建行程失败，提示 "Insufficient wallet balance"
**解决**: 
- 先给钱包充值
- 确保余额 ≥ 行程票价

### Q3: 看不到可接单的行程
**解决**: 
- 确保已经有乘客创建了行程
- 刷新页面
- 检查行程状态是否为 `REQUESTED`

### Q4: 无法取消行程
**解决**: 
- 只能取消状态为 `REQUESTED` 的行程
- 如果司机已接单，无法取消

### Q5: 前端连接不到后端
**解决**: 
- 检查后端是否部署成功（访问 https://rideshare-backend1.onrender.com）
- 检查 `.env` 文件中的 `VITE_API_URL` 是否正确
- 重启前端服务器

---

## 🔧 开发者信息

### 后端 API 端点

#### 用户相关:
- `POST /api/users` - 注册用户
- `GET /api/users/{id}` - 获取用户信息
- `POST /api/users/login` - 登录
- `POST /api/users/{id}/wallet` - 充值钱包

#### 行程相关:
- `POST /api/rides` - 创建行程
- `GET /api/rides` - 获取可接单行程
- `GET /api/rides/{id}` - 获取行程详情
- `GET /api/rides/rider/{riderId}` - 获取乘客历史
- `GET /api/rides/driver/{driverId}` - 获取司机历史
- `POST /api/rides/{id}/accept` - 接单
- `POST /api/rides/{id}/start` - 开始行程
- `POST /api/rides/{id}/complete` - 完成行程
- `POST /api/rides/{id}/cancel` - 取消行程

### 环境变量
```env
# Frontend (.env)
VITE_API_URL=https://rideshare-backend1.onrender.com/api

# Backend (Render)
DATABASE_URL=postgresql://username:password@host:5432/database
```

---

## 📚 技术栈

### 后端
- Java 17
- Servlet API 4.0
- PostgreSQL
- Tomcat (webapp-runner)
- Docker (Render deployment)

### 前端
- React 18.2.0
- Vite 7.1.10
- React Router 6.20.0
- Axios 1.6.2

---

## 🎯 测试数据

### 已创建的测试账号（来自 API 测试）:

#### 乘客:
- Email: `testrider9363@test.com`
- Password: `test123`
- 钱包: $40 (已完成 $60 的行程)

#### 司机:
- Email: `driver8929@test.com`
- Password: `driver123`
- 钱包: $60 (完成了 1 个行程)

**建议**: 创建新的测试账号进行完整测试

---

## 📞 支持

遇到问题？
1. 查看浏览器控制台 (F12) 的错误信息
2. 查看 Render 后端日志
3. 参考 `COMPLETE_TEST_RESULTS.md` 文件

---

**最后更新**: 2025-10-15  
**版本**: 1.0  
**状态**: ✅ Production Ready
