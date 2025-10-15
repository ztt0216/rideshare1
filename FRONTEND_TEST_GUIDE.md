# 🧪 本地前端测试指南

## 前提条件
✅ 后端已成功部署到Render并显示"Live"状态

## 📝 步骤1: 获取后端URL

1. 打开Render Dashboard
2. 进入您的 `rideshare-backend` 服务
3. 在页面顶部找到您的服务URL，格式类似：
   ```
   https://rideshare-backend-xxxx.onrender.com
   ```
4. **复制这个URL**

## 📝 步骤2: 配置前端环境变量

在 `frontend` 目录创建 `.env` 文件：

```powershell
cd d:\learning\2025sm2\SWEN90007\rideshare1\frontend

# 创建.env文件（将YOUR_RENDER_URL替换为实际URL）
@"
REACT_APP_API_URL=https://YOUR_RENDER_URL.onrender.com/api
"@ | Out-File -FilePath .env -Encoding utf8
```

**示例（替换成您的实际URL）：**
```
REACT_APP_API_URL=https://rideshare-backend-abcd.onrender.com/api
```

## 📝 步骤3: 测试后端连接

在启动前端之前，先测试后端是否可访问：

```powershell
# 测试后端健康检查（替换为您的URL）
curl https://YOUR_RENDER_URL.onrender.com/api/users/test

# 如果返回类似 {"message":"Test endpoint working"} 表示后端正常
```

## 📝 步骤4: 启动前端

```powershell
cd d:\learning\2025sm2\SWEN90007\rideshare1\frontend

# 启动开发服务器
npm start
```

前端会自动在浏览器中打开 `http://localhost:3000`

## 📝 步骤5: 测试完整流程

### 5.1 注册乘客账号
1. 点击 "Register"
2. 填写信息：
   - Username: `alice`
   - Email: `alice@example.com`
   - Password: `password123`
   - Role: 选择 `Rider`
3. 点击 "Register"
4. 应该会自动跳转到乘客Dashboard

### 5.2 添加钱包余额
1. 在Rider Dashboard中
2. 找到 "Add to Wallet" 输入框
3. 输入金额：`500`
4. 点击 "Add Funds"
5. 余额应该显示为 $500.00

### 5.3 请求行程
1. 在 "Request a Ride" 区域
2. 填写：
   - Pickup: `Melbourne CBD`
   - Dropoff: `Melbourne Airport`
   - Estimated Fare: `60`
3. 点击 "Request Ride"
4. 应该能看到新创建的行程，状态为 "PENDING"

### 5.4 注册司机账号（新浏览器窗口）
1. 打开**无痕/隐私浏览窗口**（或另一个浏览器）
2. 访问 `http://localhost:3000`
3. 点击 "Register"
4. 填写：
   - Username: `bob`
   - Email: `bob@example.com`
   - Password: `password123`
   - Role: 选择 `Driver`
5. 注册成功后进入Driver Dashboard

### 5.5 接受行程
1. 在Driver Dashboard中，应该能看到Alice的行程请求
2. 点击 "Accept" 按钮
3. 状态应该变为 "ACCEPTED"

### 5.6 开始行程
1. 点击 "Start Trip" 按钮
2. 状态应该变为 "IN_PROGRESS"

### 5.7 完成行程
1. 点击 "Complete Trip" 按钮
2. 状态应该变为 "COMPLETED"
3. 查看钱包余额变化：
   - 司机应该增加 $60
   - 回到Alice的窗口，余额应该减少到 $440

## ✅ 成功标志

如果一切正常，您应该看到：

✅ 前端成功启动在 http://localhost:3000
✅ 可以注册用户（乘客和司机）
✅ 乘客可以添加钱包余额
✅ 乘客可以请求行程
✅ 司机可以看到并接受行程
✅ 司机可以开始和完成行程
✅ 钱包余额正确更新

## 🐛 常见问题

### 前端无法连接后端
**现象**: 控制台显示 `Network Error` 或 `CORS error`

**解决方案**:
1. 检查 `.env` 文件中的URL是否正确
2. 确认后端在Render上显示 "Live"
3. 测试后端URL是否可访问：
   ```powershell
   curl https://your-backend.onrender.com/api/users/test
   ```
4. 如果后端刚部署，等待30-60秒让服务完全启动

### CORS错误
**现象**: 浏览器控制台显示 "Access-Control-Allow-Origin" 错误

**原因**: 后端CORS配置问题

**解决方案**: 
后端web.xml已配置允许所有来源，应该不会有此问题。如果出现：
1. 检查后端日志
2. 确认后端正在运行
3. 尝试在浏览器中直接访问后端API

### 前端启动失败
**现象**: `npm start` 报错

**解决方案**:
```powershell
# 删除node_modules重新安装
cd frontend
Remove-Item -Recurse -Force node_modules
npm install
npm start
```

### 后端第一次请求很慢
**现象**: 第一个API请求需要30-60秒

**原因**: Render免费版在15分钟无活动后会休眠

**解决方案**: 这是正常的，等待服务唤醒即可。后续请求会很快。

### 余额没有正确更新
**现象**: 完成行程后余额没变化

**检查项**:
1. 查看浏览器控制台是否有错误
2. 检查后端日志
3. 确认行程状态正确转换
4. 刷新页面重新加载数据

## 📊 调试技巧

### 查看前端控制台
按 `F12` 打开浏览器开发者工具：
- **Console**: 查看JavaScript错误和API请求
- **Network**: 查看HTTP请求和响应
- **Application**: 查看localStorage中的用户数据

### 查看后端日志
在Render Dashboard中：
1. 进入您的服务
2. 点击 "Logs" 标签
3. 查看实时日志

### 测试API直接调用
使用curl或Postman测试后端API：
```powershell
# 注册用户
curl -X POST https://your-backend.onrender.com/api/users/register `
  -H "Content-Type: application/json" `
  -d '{"username":"test","email":"test@test.com","password":"123","role":"RIDER"}'

# 登录
curl -X POST https://your-backend.onrender.com/api/users/login `
  -H "Content-Type: application/json" `
  -d '{"username":"test","password":"123"}'
```

## 🎯 快速参考

### 环境变量位置
```
frontend/.env
```

### 环境变量格式
```
REACT_APP_API_URL=https://your-backend.onrender.com/api
```

### 启动命令
```powershell
cd frontend
npm start
```

### 访问地址
- **前端**: http://localhost:3000
- **后端**: https://your-backend.onrender.com

---

**准备好了吗？从步骤1开始，获取您的后端URL！** 🚀
