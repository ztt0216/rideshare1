# 🎯 系统测试指南 - 前后端已连接

## ✅ 当前状态

- ✅ **后端**: https://rideshare-backend1.onrender.com (已部署在Render)
- ✅ **前端**: http://localhost:3000 (Vite开发服务器运行中)
- ✅ **已配置**: 前端已连接到Render后端

## 🧪 完整测试流程

### 步骤1: 注册乘客账号

1. 打开浏览器访问: **http://localhost:3000**
2. 点击 **"Register"**
3. 填写信息：
   ```
   Username: alice
   Email: alice@example.com
   Password: password123
   Role: Rider (乘客)
   ```
4. 点击 **"Register"** 按钮
5. ✅ 应该自动跳转到 Rider Dashboard

### 步骤2: 添加钱包余额

1. 在 Rider Dashboard 中找到 **"Wallet"** 区域
2. 当前余额应该显示: **$0.00**
3. 在 "Add to Wallet" 输入框输入: **500**
4. 点击 **"Add Funds"** 按钮
5. ✅ 余额应该更新为: **$500.00**
6. ✅ 导航栏中的余额也应该更新

### 步骤3: 请求行程

1. 在 **"Request a Ride"** 区域填写：
   ```
   Pickup Location: Melbourne CBD
   Dropoff Location: Melbourne Airport
   Estimated Fare: 60
   ```
2. 点击 **"Request Ride"** 按钮
3. ✅ 应该看到成功消息
4. ✅ 在 "My Rides" 列表中看到新创建的行程
5. ✅ 行程状态应该是: **PENDING** (黄色)

### 步骤4: 注册司机账号（新浏览器窗口）

⚠️ **重要**: 使用无痕/隐私窗口或另一个浏览器

1. 打开**无痕窗口** (Ctrl+Shift+N 或 Ctrl+Shift+P)
2. 访问: **http://localhost:3000**
3. 点击 **"Register"**
4. 填写信息：
   ```
   Username: bob
   Email: bob@example.com
   Password: password123
   Role: Driver (司机)
   ```
5. 点击 **"Register"** 按钮
6. ✅ 应该跳转到 Driver Dashboard

### 步骤5: 司机接受行程

1. 在 Driver Dashboard 中，应该能看到 **"Available Rides"** 列表
2. 找到Alice的行程请求：
   ```
   From: Melbourne CBD
   To: Melbourne Airport
   Fare: $60.00
   Status: PENDING
   ```
3. 点击 **"Accept"** 按钮
4. ✅ 状态应该变为: **ACCEPTED** (蓝色)
5. ✅ 行程应该移到 "My Active Rides" 区域

### 步骤6: 司机开始行程

1. 在 "My Active Rides" 中找到刚接受的行程
2. 点击 **"Start Trip"** 按钮
3. ✅ 状态应该变为: **IN_PROGRESS** (绿色)

### 步骤7: 司机完成行程

1. 点击 **"Complete Trip"** 按钮
2. ✅ 状态应该变为: **COMPLETED** (深绿色)
3. ✅ 司机钱包余额应该显示: **$60.00**

### 步骤8: 验证乘客余额

1. 切换回Alice的浏览器窗口
2. 刷新页面或点击导航栏查看余额
3. ✅ 余额应该从 $500.00 减少到: **$440.00**
4. ✅ 在 "My Rides" 中该行程状态应该是: **COMPLETED**

## ✅ 成功标志

如果看到以下所有现象，说明系统运行正常：

- ✅ Alice (乘客) 注册成功
- ✅ Alice 添加 $500 到钱包
- ✅ Alice 请求行程 (Melbourne CBD → Airport, $60)
- ✅ Bob (司机) 注册成功
- ✅ Bob 看到并接受Alice的行程
- ✅ Bob 开始行程
- ✅ Bob 完成行程并获得 $60
- ✅ Alice 余额减少到 $440
- ✅ 行程状态正确转换: PENDING → ACCEPTED → IN_PROGRESS → COMPLETED

## 🐛 故障排查

### 前端无法连接后端

**症状**: 控制台显示网络错误

**检查**:
```powershell
# 测试后端是否在线
curl https://rideshare-backend1.onrender.com
```

**解决方案**:
1. 确认后端在Render上显示 "Live"
2. 检查 `.env` 文件中的URL是否正确
3. 如果后端刚部署或长时间未使用，等待30-60秒让服务唤醒

### 注册失败

**症状**: 点击注册后没有反应或报错

**检查**:
1. 打开浏览器开发者工具 (F12)
2. 查看 Console 标签的错误信息
3. 查看 Network 标签的API请求

**常见原因**:
- 用户名或邮箱已存在
- 后端服务休眠（等待唤醒）
- CORS问题（检查后端日志）

### 余额没有更新

**症状**: 添加资金后余额不变

**解决方案**:
1. 刷新页面重新加载
2. 检查浏览器控制台是否有错误
3. 检查API请求是否成功 (Network标签)
4. 注销后重新登录

### 司机看不到行程

**症状**: Driver Dashboard中没有可用行程

**检查**:
1. 确认乘客已成功创建行程
2. 刷新司机页面
3. 检查行程状态（只有PENDING状态的行程才会显示）

### 行程状态没有更新

**症状**: 点击按钮后状态不变

**解决方案**:
1. 刷新页面
2. 检查浏览器控制台错误
3. 确认后端日志没有错误
4. 重新执行操作

## 📊 浏览器开发者工具

按 **F12** 打开开发者工具：

### Console 标签
查看JavaScript错误和日志：
```
✓ 成功: 绿色消息
✗ 错误: 红色错误信息
```

### Network 标签
查看API请求：
1. 刷新页面开始记录
2. 点击具体请求查看详情
3. 查看 Request/Response 数据

### Application 标签
查看存储的数据：
1. 左侧选择 "Local Storage"
2. 选择 http://localhost:3000
3. 查看 "user" 键的值

## 🎯 快速测试命令

### 测试后端健康状态
```powershell
curl https://rideshare-backend1.onrender.com
```

### 重启前端
```powershell
# 停止Vite (Ctrl+C)
# 重新启动
cd d:\learning\2025sm2\SWEN90007\rideshare1\frontend-new
npm run dev
```

### 查看环境配置
```powershell
Get-Content d:\learning\2025sm2\SWEN90007\rideshare1\frontend-new\.env
```

## 🌐 访问地址

- **前端**: http://localhost:3000
- **后端**: https://rideshare-backend1.onrender.com
- **后端API**: https://rideshare-backend1.onrender.com/api

## 📝 测试记录表

| 步骤 | 操作 | 预期结果 | 实际结果 | 状态 |
|------|------|---------|---------|------|
| 1 | 注册乘客Alice | 跳转到Dashboard | | ☐ |
| 2 | 添加$500 | 余额显示$500 | | ☐ |
| 3 | 请求行程 | 显示PENDING行程 | | ☐ |
| 4 | 注册司机Bob | 跳转到Dashboard | | ☐ |
| 5 | 接受行程 | 状态变为ACCEPTED | | ☐ |
| 6 | 开始行程 | 状态变为IN_PROGRESS | | ☐ |
| 7 | 完成行程 | Bob获得$60 | | ☐ |
| 8 | 验证Alice余额 | 余额变为$440 | | ☐ |

---

**准备好了吗？打开浏览器访问 http://localhost:3000 开始测试！** 🚀

**Vite提示**: 代码改动会立即反映在浏览器中，无需手动刷新！⚡
