# Render Backend Deployment Guide

## Prerequisites
- GitHub account
- Render account (https://render.com)
- PostgreSQL database on Render (already created)

## Step 1: Prepare Repository

### 1.1 Check Git Status
Make sure all changes are committed to Git:
```bash
git status
git add .
git commit -m "Prepare for Render deployment"
git push origin main
```

### 1.2 Verify Required Files
The following files are already in your repository:
- ✅ `Procfile` - Tells Render how to start the app
- ✅ `system.properties` - Specifies Java version (17)
- ✅ `pom.xml` - Maven configuration with webapp-runner

## Step 2: Create Web Service on Render

### 2.1 Go to Render Dashboard
1. Visit https://dashboard.render.com
2. Click "New +" button
3. Select "Web Service"

### 2.2 Connect GitHub Repository
1. Click "Connect GitHub" (if not already connected)
2. Select your repository: `ztt0216/rideshare1`
3. Click "Connect"

### 2.3 Configure Web Service

Fill in the following settings:

**Basic Settings:**
- **Name**: `rideshare-backend` (or your preferred name)
- **Region**: Select closest to you (e.g., Oregon, Singapore)
- **Branch**: `main`
- **Root Directory**: Leave empty (root of repo)
- **Runtime**: `Java`

**Build Settings:**
- **Build Command**: 
  ```
  mvn clean package
  ```
- **Start Command**: 
  ```
  java $JAVA_OPTS -jar target/dependency/webapp-runner.jar --port $PORT target/rideshare1.war
  ```

**Instance Type:**
- Select **Free** tier (for testing)

### 2.4 Add Environment Variables

Click "Advanced" and add the following environment variables:

| Key | Value | Notes |
|-----|-------|-------|
| `DATABASE_URL` | Your Render PostgreSQL URL | From your PostgreSQL service |
| `JAVA_OPTS` | `-Xmx512m -Xms256m` | Memory settings for free tier |

**How to get DATABASE_URL:**
1. Go to your PostgreSQL service in Render dashboard
2. Copy the "External Database URL"
3. Format: `postgresql://user:password@host:5432/database`

### 2.5 Health Check (Optional)
- **Health Check Path**: `/api/users/test` (if you have a health endpoint)
- Or leave empty for default

### 2.6 Deploy
1. Click "Create Web Service"
2. Render will automatically:
   - Clone your repository
   - Run `mvn clean package`
   - Download webapp-runner
   - Start your application
   - Assign a public URL (e.g., https://rideshare-backend.onrender.com)

## Step 3: Monitor Deployment

### 3.1 Watch Build Logs
- The "Logs" tab shows real-time build progress
- Maven will download dependencies (first build takes 3-5 minutes)
- Look for: "INFO: Starting ProtocolHandler"

### 3.2 Common Build Issues

**Issue 1: Build timeout**
- Free tier has 15-minute build limit
- Solution: Build should complete in ~5 minutes if dependencies are cached

**Issue 2: Memory issues**
- Error: "Java heap space"
- Solution: Adjust JAVA_OPTS to `-Xmx450m` (lower memory)

**Issue 3: Database connection**
- Error: "Connection refused"
- Solution: Check DATABASE_URL format and database is running

### 3.3 Verify Deployment
Once deployed, test your API:
```bash
# Replace with your actual Render URL
curl https://rideshare-backend.onrender.com/api/users/test

# Test user registration
curl -X POST https://rideshare-backend.onrender.com/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123","email":"test@example.com","role":"RIDER"}'
```

## Step 4: Database Connection

### 4.1 Verify DATABASE_URL Format
The DATABASE_URL from Render should be in this format:
```
postgresql://user:password@host:5432/database
```

### 4.2 Update Database Configuration (if needed)
If your app uses different environment variable names, you may need to parse DATABASE_URL in your code or add individual variables:
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`

### 4.3 Check Connection Pool Settings
Your `DatabaseConnection.java` should handle connection pooling efficiently for cloud deployment.

## Step 5: Configure CORS for Frontend

### 5.1 Update CORS Settings
Once deployed, you'll need to update your CORS configuration in `web.xml` or your CORS filter to allow requests from:
- `http://localhost:3000` (local development)
- Your future frontend URL on Render/Vercel/Netlify

Example in web.xml:
```xml
<filter>
    <filter-name>CorsFilter</filter-name>
    <filter-class>com.thetransactioncompany.cors.CORSFilter</filter-class>
    <init-param>
        <param-name>cors.allowOrigin</param-name>
        <param-value>http://localhost:3000,https://your-frontend.vercel.app</param-value>
    </init-param>
</filter>
```

## Step 6: Update Frontend Configuration

After backend is deployed, update your frontend's API base URL:

**In `frontend/src/services/api.js`:**
```javascript
const API_BASE_URL = process.env.REACT_APP_API_URL || 'https://rideshare-backend.onrender.com/api';
```

**Create `.env` file in frontend:**
```
REACT_APP_API_URL=https://rideshare-backend.onrender.com/api
```

## Useful Commands

### Redeploy
Render automatically redeploys on every git push to main:
```bash
git add .
git commit -m "Update backend"
git push origin main
```

### Manual Deploy
In Render dashboard:
1. Go to your web service
2. Click "Manual Deploy" → "Deploy latest commit"

### View Logs
- Go to "Logs" tab in Render dashboard
- Or use Render CLI (optional)

## Important Notes

### Free Tier Limitations
- **Sleep after 15 minutes of inactivity**
  - First request after sleep takes 30-60 seconds to wake up
  - Consider upgrading to paid tier ($7/month) for 24/7 uptime
  
- **750 hours/month free** (enough for one service)

- **Build time limit: 15 minutes**

### Database Connection
- Use connection pooling to handle multiple requests efficiently
- Close connections properly to avoid leaks
- Free PostgreSQL on Render:
  - 90-day expiration (data is deleted)
  - 1GB storage
  - Consider upgrading for production

### Security
- Never commit DATABASE_URL to Git
- Use environment variables for all secrets
- Enable HTTPS (Render provides free SSL)

## Troubleshooting

### App won't start
1. Check logs for errors
2. Verify DATABASE_URL is correct
3. Ensure Maven build completed successfully
4. Check Java version matches (Java 17)

### Database connection errors
1. Verify PostgreSQL service is running on Render
2. Check DATABASE_URL format
3. Test connection from Render shell (if available)
4. Ensure database exists and user has permissions

### 502 Bad Gateway
1. App failed to start within timeout
2. Check start command is correct
3. Verify PORT environment variable is used correctly

### Changes not reflecting
1. Ensure git push succeeded
2. Check Render triggered new deployment
3. Clear browser cache
4. Force manual deploy if needed

## Next Steps

After successful backend deployment:

1. ✅ **Backend deployed on Render**
2. 🔄 **Update frontend to use Render backend URL**
3. 🔄 **Deploy frontend to Vercel/Netlify**
4. 🔄 **Update CORS configuration**
5. 🔄 **End-to-end testing**

---

**Your Backend URL (after deployment):**
```
https://rideshare-backend.onrender.com
```

Update this URL in your frontend configuration!
