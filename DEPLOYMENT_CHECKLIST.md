# Backend Deployment Checklist

## ✅ Pre-deployment Checklist

### 1. Code Preparation
- [x] DatabaseConfig.java updated to use environment variables
- [x] CORS configured in web.xml
- [x] Procfile exists with correct start command
- [x] system.properties specifies Java 17
- [x] pom.xml includes webapp-runner dependency

### 2. Git Repository
- [ ] All changes committed
- [ ] Pushed to GitHub main branch

### 3. Database Setup
- [x] PostgreSQL database created on Render
- [x] Database URL: `dpg-d3ne6rjipnbc73b1a8a0-a.oregon-postgres.render.com`
- [x] Database name: `rideshare1`
- [x] Database user: `rideshare1_user`

## 📝 Deployment Steps

### Step 1: Commit and Push Code
```bash
cd d:\learning\2025sm2\SWEN90007\rideshare1
git add .
git commit -m "Prepare backend for Render deployment"
git push origin main
```

### Step 2: Create Render Web Service
1. Go to https://dashboard.render.com
2. Click "New +" → "Web Service"
3. Connect to GitHub repository: `ztt0216/rideshare1`
4. Configure:
   - **Name**: `rideshare-backend`
   - **Branch**: `main`
   - **Runtime**: `Java`
   - **Build Command**: `mvn clean package`
   - **Start Command**: `java $JAVA_OPTS -jar target/dependency/webapp-runner.jar --port $PORT target/rideshare1.war`

### Step 3: Set Environment Variables
In Render dashboard, add:
```
DATABASE_URL=postgresql://rideshare1_user:hqhmQ01YswRr4Z1Oyk7A0cha44DkT9cC@dpg-d3ne6rjipnbc73b1a8a0-a.oregon-postgres.render.com/rideshare1
JAVA_OPTS=-Xmx512m -Xms256m
```

### Step 4: Deploy and Monitor
1. Click "Create Web Service"
2. Monitor build logs
3. Wait for "Your service is live 🎉"
4. Note your service URL (e.g., `https://rideshare-backend.onrender.com`)

### Step 5: Test Deployment
```bash
# Test health endpoint (replace with your Render URL)
curl https://rideshare-backend.onrender.com/api/users/test

# Test user registration
curl -X POST https://rideshare-backend.onrender.com/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "email": "test@example.com",
    "role": "RIDER"
  }'
```

## 🔧 Post-deployment Tasks

### 1. Update Frontend Configuration
After backend is deployed, update frontend to use the new backend URL:

**File: `frontend/.env`** (create if doesn't exist)
```
REACT_APP_API_URL=https://rideshare-backend.onrender.com/api
```

**File: `frontend/src/services/api.js`**
Update the base URL to use environment variable:
```javascript
const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
```

### 2. Test Integration
1. Run frontend locally: `npm start`
2. Test with deployed backend
3. Verify API calls work correctly

### 3. Deploy Frontend (Next Step)
Options:
- **Vercel**: Best for React apps, free tier, automatic deployments
- **Netlify**: Similar to Vercel, also free tier
- **Render**: Keep everything in one place

## ⚠️ Important Notes

### Free Tier Limitations
- Service sleeps after 15 minutes of inactivity
- First request after sleep takes 30-60 seconds
- 750 hours/month free (one service runs 24/7)

### Database
- Your PostgreSQL database on Render has 90-day expiration on free tier
- Consider upgrading to paid tier for production ($7/month)

### Security
- DATABASE_URL is set as environment variable (not in code) ✅
- CORS allows all origins (*) - consider restricting for production
- Consider adding authentication middleware

## 🐛 Troubleshooting

### Build Fails
- Check Maven logs in Render dashboard
- Verify Java version is 17
- Ensure all dependencies are in pom.xml

### App Won't Start
- Check start command in Render dashboard
- Verify webapp-runner.jar is downloaded
- Check logs for port binding issues

### Database Connection Errors
- Verify DATABASE_URL format
- Test database is accessible from Render
- Check database credentials

### 502 Bad Gateway
- App didn't start within timeout
- Check logs for startup errors
- Verify PORT environment variable is used

## 📊 Monitoring

### Check Service Status
- Render Dashboard → Your Service → "Logs" tab
- Monitor for errors or warnings
- Check response times

### View Logs
```
# In Render dashboard
Events tab: Deployment history
Logs tab: Real-time application logs
Metrics tab: CPU, Memory usage
```

## 🎯 Success Criteria

Backend deployment is successful when:
- [x] Build completes without errors
- [x] Service shows "Live" status
- [x] API endpoints respond correctly
- [x] Database connections work
- [x] CORS allows frontend requests

## 📚 Reference

- **Render Docs**: https://render.com/docs
- **Java on Render**: https://render.com/docs/deploy-java
- **Environment Variables**: https://render.com/docs/environment-variables
- **Troubleshooting**: https://render.com/docs/troubleshooting

---

**Current Status:**
- ✅ Code prepared for deployment
- ⏳ Waiting for Git push
- ⏳ Waiting for Render service creation

**Next Action:** Commit and push code to GitHub, then follow Step 2 above.
