# 🚀 Vite + React Frontend 快速启动指南

## ✨ Vite的优势

相比Create React App (CRA):
- ⚡ **极快的启动速度**: 秒级启动，而不是分钟
- 🔥 **即时热更新 (HMR)**: 代码保存后立即在浏览器中更新
- 📦 **更小的包体积**: 优化的构建输出
- 🛠️ **现代化工具链**: 原生ES模块支持
- 🎯 **更少的依赖**: 安装更快，node_modules更小

## 📁 项目结构

```
frontend-new/
├── src/
│   ├── components/         # React组件
│   │   ├── Login.js
│   │   ├── Register.js
│   │   ├── RiderDashboard.js
│   │   └── DriverDashboard.js
│   ├── services/           # API服务
│   │   └── api.js
│   ├── App.jsx             # 主应用组件
│   ├── App.css             # 应用样式
│   ├── main.jsx            # 入口文件
│   └── index.css           # 全局样式
├── .env                    # 环境变量（已创建）
├── .env.example            # 环境变量示例
├── vite.config.js          # Vite配置
└── package.json            # 项目依赖
```

## 🎯 步骤1: 配置后端URL

### 方案A: 使用Render部署的后端（推荐）

编辑 `.env` 文件，填入您的Render后端URL：

```env
VITE_API_URL=https://your-backend.onrender.com/api
```

**获取URL步骤：**
1. 打开 Render Dashboard
2. 进入您的 `rideshare-backend` 服务
3. 复制页面顶部的URL（例如：`https://rideshare-backend-abcd.onrender.com`）
4. 在URL后面添加 `/api`

### 方案B: 使用本地后端（开发）

将 `.env` 留空或注释掉：
```env
# VITE_API_URL=
```

这样会使用 `vite.config.js` 中的proxy配置，自动转发到 `http://localhost:8080`

## 🚀 步骤2: 启动前端

```powershell
cd d:\learning\2025sm2\SWEN90007\rideshare1\frontend-new

# 启动开发服务器
npm run dev
```

**预期输出：**
```
VITE v7.1.10  ready in 451 ms

➜  Local:   http://localhost:3000/
➜  Network: use --host to expose
➜  press h + enter to show help
```

浏览器会自动打开 `http://localhost:3000` 🎉

## 🧪 步骤3: 测试应用

### 3.1 注册乘客账号
1. 点击 "Register"
2. 填写信息：
   - Username: `alice`
   - Email: `alice@example.com`
   - Password: `password123`
   - Role: `Rider`
3. 提交注册

### 3.2 添加钱包余额
1. 在Rider Dashboard中
2. 输入金额：`500`
3. 点击 "Add Funds"

### 3.3 请求行程
1. 填写行程信息：
   - Pickup: `Melbourne CBD`
   - Dropoff: `Melbourne Airport`
   - Fare: `60`
2. 点击 "Request Ride"

### 3.4 注册司机账号（新窗口）
1. 打开**无痕窗口**
2. 访问 `http://localhost:3000`
3. 注册司机账号（Username: `bob`）

### 3.5 完成行程流程
1. 司机接受行程
2. 司机开始行程
3. 司机完成行程
4. 查看钱包余额变化

## ⚡ Vite特性

### 热模块替换 (HMR)
修改任何文件后保存，浏览器**立即更新**，无需刷新！

### 快速构建
```powershell
# 生产环境构建
npm run build

# 预览构建结果
npm run preview
```

### 环境变量
- **开发环境**: `.env`
- **生产环境**: `.env.production`
- **所有环境变量必须以 `VITE_` 开头**

示例：
```javascript
import.meta.env.VITE_API_URL  // 读取环境变量
```

## 🔧 Vite配置说明

### vite.config.js
```javascript
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,              // 开发服务器端口
    proxy: {
      '/api': {              // API代理
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
})
```

### 代理工作原理
当 `VITE_API_URL` 未设置时：
- 前端请求: `http://localhost:3000/api/users`
- Vite代理到: `http://localhost:8080/api/users`
- 解决CORS问题

## 📊 性能对比

| 特性 | Create React App | Vite |
|------|------------------|------|
| 启动时间 | 30-60秒 | <1秒 |
| 热更新速度 | 1-3秒 | <100ms |
| 构建时间 | 2-5分钟 | 30-60秒 |
| node_modules大小 | ~300MB | ~150MB |
| 包数量 | 1300+ | 180+ |

## 🐛 常见问题

### 端口3000被占用
```powershell
# 修改 vite.config.js 中的port:
server: {
  port: 5173,  # 使用其他端口
}
```

### 环境变量不生效
1. 确保变量名以 `VITE_` 开头
2. 修改 `.env` 后需要**重启开发服务器**
3. 使用 `import.meta.env.VITE_XXX` 而不是 `process.env`

### CORS错误
1. 检查后端URL是否正确
2. 确认后端CORS配置
3. 如果使用本地后端，确保proxy配置正确

### 组件不更新
1. 检查浏览器控制台是否有错误
2. 确保组件文件名大写开头
3. 清除浏览器缓存

## 📝 可用命令

```powershell
npm run dev       # 启动开发服务器
npm run build     # 构建生产版本
npm run preview   # 预览构建结果
npm run lint      # 代码检查（如果配置了）
```

## 🎨 样式系统

- **全局样式**: `src/index.css`
- **应用样式**: `src/App.css`
- **组件样式**: 内联在组件文件中

## 🔄 从CRA迁移的变化

1. **环境变量**: `REACT_APP_*` → `VITE_*`
2. **入口文件**: `src/index.js` → `src/main.jsx`
3. **HTML模板**: `public/index.html` → `index.html`（移到根目录）
4. **访问环境变量**: `process.env` → `import.meta.env`

## ✅ 快速检查清单

- [ ] `.env` 文件已配置后端URL
- [ ] 运行 `npm run dev` 成功启动
- [ ] 浏览器打开 `http://localhost:3000`
- [ ] 可以注册用户
- [ ] 可以添加钱包余额
- [ ] 可以请求行程
- [ ] HMR正常工作（修改文件立即更新）

## 🚀 下一步

1. ✅ 前端使用Vite运行
2. ✅ 连接到Render后端
3. 🔄 测试完整功能
4. 🔄 部署前端到Vercel/Netlify（可选）

---

**准备好了吗？执行步骤1配置后端URL，然后运行 `npm run dev`！** ⚡
