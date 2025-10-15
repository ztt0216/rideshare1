# Frontend Deployment Guide - Render

## 🚀 部署步骤

### 方法 1: 通过 Render Dashboard（推荐）

#### 步骤 1: 创建新的静态站点服务
1. 登录 Render: https://dashboard.render.com
2. 点击 **"New +"** → 选择 **"Static Site"**

#### 步骤 2: 连接 GitHub 仓库
1. 选择你的 GitHub 仓库: `ztt0216/rideshare1`
2. 点击 **"Connect"**

#### 步骤 3: 配置构建设置
填写以下信息:

| 字段 | 值 |
|------|-----|
| **Name** | `rideshare-frontend` |
| **Region** | Singapore (或离你最近的区域) |
| **Branch** | `main` |
| **Root Directory** | `frontend-new` |
| **Build Command** | `npm install && npm run build` |
| **Publish Directory** | `dist` |

#### 步骤 4: 添加环境变量
点击 **"Advanced"** → 添加环境变量:
```
VITE_API_URL=https://rideshare-backend1.onrender.com/api
```

#### 步骤 5: 部署
1. 点击 **"Create Static Site"**
2. 等待构建完成（约 2-3 分钟）
3. 部署成功后会得到一个 URL，类似: `https://rideshare-frontend.onrender.com`

---

### 方法 2: 使用 render.yaml（自动部署）

如果你已经在仓库根目录有 `render.yaml`，可以合并前端配置。

**现在的前端配置已经在 `frontend-new/render.yaml` 中了**

---

## 📝 配置文件说明

### 1. `.env.production`
生产环境变量配置:
```env
VITE_API_URL=https://rideshare-backend1.onrender.com/api
```

### 2. `vite.config.js`
Vite 构建配置:
```javascript
export default defineConfig({
  plugins: [react()],
  build: {
    outDir: 'dist',
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom', 'react-router-dom'],
          axios: ['axios']
        }
      }
    }
  }
})
```

**优化说明**:
- `outDir: 'dist'`: 输出到 dist 目录
- `sourcemap: false`: 生产环境不生成 sourcemap（减小体积）
- `manualChunks`: 代码分割，将第三方库单独打包

### 3. `public/_redirects`
SPA 路由重定向配置:
```
/*    /index.html   200
```
这确保所有路由都指向 index.html（React Router 需要）

---

## 🔧 本地测试

### 构建生产版本:
```bash
cd frontend-new
npm run build
```

### 预览生产版本:
```bash
npm run preview
```
然后访问: http://localhost:4173

---

## 📊 构建结果

```
dist/index.html                   0.61 kB │ gzip:  0.34 kB
dist/assets/index-C5xTePG6.css    3.72 kB │ gzip:  1.25 kB
dist/assets/axios-ngrFHoWO.js    36.01 kB │ gzip: 14.56 kB
dist/assets/vendor-C7BbO1uU.js   44.17 kB │ gzip: 15.81 kB
dist/assets/index-Cd5Xrapt.js   198.25 kB │ gzip: 61.29 kB
✓ built in 1.71s
```

**总大小**: ~282 KB (未压缩) / ~93 KB (gzip 压缩)

---

## 🌐 部署后的架构

```
┌─────────────────────────────────────────────┐
│                 用户浏览器                    │
└──────────────────┬──────────────────────────┘
                   │
                   │ HTTPS
                   ▼
┌─────────────────────────────────────────────┐
│      Render Static Site (Frontend)          │
│  https://rideshare-frontend.onrender.com    │
│                                              │
│  - React SPA                                 │
│  - Vite Build                                │
│  - CDN Cached                                │
└──────────────────┬──────────────────────────┘
                   │
                   │ API Calls
                   │ HTTPS
                   ▼
┌─────────────────────────────────────────────┐
│       Render Web Service (Backend)          │
│  https://rideshare-backend1.onrender.com    │
│                                              │
│  - Java 17 + Tomcat                          │
│  - Docker Container                          │
└──────────────────┬──────────────────────────┘
                   │
                   │ JDBC + SSL
                   ▼
┌─────────────────────────────────────────────┐
│     Render PostgreSQL (Database)            │
│                                              │
│  - PostgreSQL 15                             │
│  - Automatic Backups                         │
└─────────────────────────────────────────────┘
```

---

## ✅ 部署检查清单

部署前确认:
- [ ] `package.json` 中有正确的 `build` 脚本
- [ ] `.env.production` 包含正确的后端 API URL
- [ ] `vite.config.js` 配置了构建优化
- [ ] `public/_redirects` 文件存在
- [ ] 本地构建测试通过 (`npm run build`)
- [ ] 代码已提交并推送到 GitHub

部署后验证:
- [ ] 网站可以正常访问
- [ ] 注册功能正常
- [ ] 登录功能正常
- [ ] API 调用正常（检查浏览器控制台）
- [ ] 路由切换正常
- [ ] 样式显示正常

---

## 🐛 常见问题

### Q1: 部署后显示 404
**原因**: 缺少 `_redirects` 文件或配置错误  
**解决**: 确保 `public/_redirects` 存在并包含 `/* /index.html 200`

### Q2: API 调用失败（CORS 错误）
**原因**: 后端 CORS 配置问题  
**解决**: 确认后端 `CorsFilter` 已正确配置并允许你的前端域名

### Q3: 环境变量未生效
**原因**: Render 没有读取到环境变量  
**解决**: 
1. 在 Render Dashboard 的 Environment 页面添加环境变量
2. 重新部署

### Q4: 构建失败
**原因**: Node 版本或依赖问题  
**解决**:
1. 检查 Node 版本（推荐 18.x 或 20.x）
2. 清除缓存: Render Dashboard → Settings → Clear build cache
3. 检查 `package.json` 中的依赖版本

### Q5: 页面加载慢
**原因**: Render 免费套餐会休眠  
**解决**:
1. 升级到付费套餐
2. 或使用 UptimeRobot 等服务定期 ping

---

## 📈 性能优化建议

### 1. 启用 CDN（Render 自动启用）
Render 静态站点自动通过 CDN 分发，无需额外配置。

### 2. 代码分割
已在 `vite.config.js` 中配置:
```javascript
manualChunks: {
  vendor: ['react', 'react-dom', 'react-router-dom'],
  axios: ['axios']
}
```

### 3. 图片优化
如果有图片资源:
- 使用 WebP 格式
- 添加懒加载
- 压缩图片大小

### 4. 压缩资源
Vite 自动进行:
- JavaScript minification
- CSS minification
- Gzip 压缩

---

## 🔐 安全建议

### 1. HTTPS
Render 自动提供免费的 SSL 证书（Let's Encrypt）

### 2. 环境变量
不要在代码中硬编码敏感信息，使用环境变量:
```javascript
const API_URL = import.meta.env.VITE_API_URL;
```

### 3. 内容安全策略（CSP）
在 `render.yaml` 中已配置基本的安全头:
```yaml
headers:
  - path: /*
    name: X-Frame-Options
    value: DENY
  - path: /*
    name: X-Content-Type-Options
    value: nosniff
```

---

## 💰 费用说明

### Render 免费套餐限制:
- ✅ 静态站点: **完全免费**
- ✅ 自动 HTTPS
- ✅ CDN 加速
- ✅ 自动部署
- ✅ 自定义域名支持
- ⚠️  无流量限制，但有带宽限制

### 升级选项（如需要）:
- **Starter**: $7/月 - 更高性能
- **Standard**: $25/月 - 生产环境推荐

---

## 📞 下一步

部署完成后:
1. 获取前端 URL（例如: `https://rideshare-frontend.onrender.com`）
2. 更新文档和 README
3. 测试完整的用户流程
4. 如需要，配置自定义域名

---

## 🔗 相关链接

- **Frontend URL**: https://rideshare-frontend.onrender.com (部署后填写)
- **Backend URL**: https://rideshare-backend1.onrender.com
- **GitHub**: https://github.com/ztt0216/rideshare1
- **Render Dashboard**: https://dashboard.render.com

---

**最后更新**: 2025-10-15  
**版本**: 1.0  
**状态**: 准备部署
