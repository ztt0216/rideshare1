# 🚀 前端部署到 Render - 快速步骤

## 准备工作（已完成 ✅）

- ✅ 前端已迁移到 Vite (`frontend-new/`)
- ✅ 构建测试通过 (`npm run build`)
- ✅ 生产环境配置已创建 (`.env.production`)
- ✅ Render 配置文件已创建 (`render.yaml`)
- ✅ SPA 路由配置已添加 (`public/_redirects`)
- ✅ Vite 构建优化已配置 (`vite.config.js`)

---

## 🎯 部署步骤（手动推送后）

### 步骤 1: 提交代码到 GitHub

```bash
# 在项目根目录
cd d:\learning\2025sm2\SWEN90007\rideshare1

# 可选：删除旧的 frontend 文件夹
git rm -rf frontend

# 添加所有新文件
git add -A

# 提交
git commit -m "Add frontend deployment config for Render"

# 推送
git push origin main
```

### 步骤 2: 在 Render 创建静态站点

1. 访问: https://dashboard.render.com
2. 点击 **"New +"** → 选择 **"Static Site"**
3. 连接你的 GitHub 仓库: `ztt0216/rideshare1`

### 步骤 3: 配置部署设置

填写以下信息:

| 字段 | 值 |
|------|-----|
| Name | `rideshare-frontend` |
| Region | `Singapore` (或其他) |
| Branch | `main` |
| Root Directory | `frontend-new` ⚠️ **重要!** |
| Build Command | `npm install && npm run build` |
| Publish Directory | `dist` |

### 步骤 4: 添加环境变量

在 **Advanced** → **Environment Variables** 添加:

```
VITE_API_URL=https://rideshare-backend1.onrender.com/api
```

### 步骤 5: 创建并部署

1. 点击 **"Create Static Site"**
2. 等待构建完成（约 2-3 分钟）
3. 获取前端 URL（类似: `https://rideshare-frontend.onrender.com`）

---

## ✅ 部署后验证

访问你的前端 URL，测试以下功能:

1. **页面加载** ✓
   - 首页正常显示
   - 样式正确

2. **路由功能** ✓
   - 点击 Register 跳转正常
   - 点击 Login 跳转正常
   - URL 变化正确

3. **API 连接** ✓
   - 注册新用户
   - 登录测试
   - 创建行程

4. **控制台检查** (F12)
   - 无报错信息
   - API 调用成功（状态 200/201）
   - 无 CORS 错误

---

## 📋 需要推送的文件

确保以下文件已添加到 Git:

```
frontend-new/
├── .env.production              ← 生产环境变量
├── vite.config.js               ← 已更新（构建优化）
├── render.yaml                  ← Render 配置
├── public/_redirects            ← SPA 路由重定向
├── RENDER_DEPLOYMENT.md         ← 部署文档
└── (其他已有文件)

根目录新增文档:
├── CLEANUP_GUIDE.md             ← 清理指南
├── COMPLETE_TEST_RESULTS.md     ← 测试结果
├── USER_MANUAL.md               ← 用户手册
├── DATABASE_FIX.md              ← 数据库修复
└── TEST_RIDE_CREATION.md        ← 测试指南
```

---

## 🔗 最终部署架构

```
用户浏览器
    ↓
前端 (Render Static Site)
    ↓ API 调用
后端 (Render Web Service)
    ↓ JDBC
数据库 (Render PostgreSQL)
```

---

## 💡 提示

### 如果构建失败:
1. 检查 Root Directory 是否设置为 `frontend-new`
2. 检查 Build Command 是否正确
3. 查看构建日志，定位错误
4. 在 Render Dashboard 清除缓存后重试

### 如果 API 调用失败:
1. 检查环境变量 `VITE_API_URL` 是否正确
2. 检查后端 CORS 配置
3. 在浏览器控制台查看具体错误

### 自定义域名（可选）:
部署成功后，可以在 Render Dashboard → Settings → Custom Domain 添加你自己的域名。

---

## 📞 支持

- **前端**: 即将部署
- **后端**: https://rideshare-backend1.onrender.com ✅
- **GitHub**: https://github.com/ztt0216/rideshare1
- **文档**: 见项目根目录的 `*.md` 文件

---

**准备就绪！现在可以 push 代码并在 Render 上部署了！** 🚀
