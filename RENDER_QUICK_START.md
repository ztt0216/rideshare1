# Quick Deploy to Render

## Step 1: Commit and Push to GitHub

```powershell
# Navigate to project directory
cd d:\learning\2025sm2\SWEN90007\rideshare1

# Check current status
git status

# Add all changes
git add .

# Commit changes
git commit -m "Update DatabaseConfig for Render deployment with environment variables"

# Push to GitHub
git push origin main
```

## Step 2: Create Render Web Service

1. Open https://dashboard.render.com in your browser
2. Click **"New +"** button (top right)
3. Select **"Web Service"**

## Step 3: Connect Repository

1. If not already connected, click **"Connect GitHub"**
2. Search for: **ztt0216/rideshare1**
3. Click **"Connect"** next to your repository

## Step 4: Configure Service

Fill in these exact values:

### Basic Configuration
- **Name**: `rideshare-backend` (or any name you prefer)
- **Region**: `Oregon (US West)` (same as your database)
- **Branch**: `main`
- **Root Directory**: *(leave empty)*
- **Environment**: Select **"Docker"**

### Build Configuration
Docker will automatically detect the `Dockerfile` in your repository.
- **Docker Build Context Directory**: *(leave empty or `.`)*
- **Dockerfile Path**: `Dockerfile` (should auto-detect)

No need to specify build or start commands - they're in the Dockerfile!

### Instance Type
- Select: **Free** (for testing)

## Step 5: Environment Variables

Click **"Advanced"** → **"Add Environment Variable"**

Add this variable:

**Variable:**
- **Key**: `DATABASE_URL`
- **Value**: `postgresql://rideshare1_user:hqhmQ01YswRr4Z1Oyk7A0cha44DkT9cC@dpg-d3ne6rjipnbc73b1a8a0-a.oregon-postgres.render.com/rideshare1`

Note: `JAVA_OPTS` and `PORT` are automatically handled by the Dockerfile.

## Step 6: Deploy!

1. Click **"Create Web Service"** button at the bottom
2. Wait for build to complete (3-5 minutes)
3. Look for "Your service is live 🎉" message

## Step 7: Get Your Service URL

After deployment, you'll see your service URL at the top:
```
https://rideshare-backend-XXXX.onrender.com
```

Copy this URL - you'll need it for the frontend!

## Step 8: Test Your API

Replace `YOUR_URL` with your actual Render URL:

```powershell
# Test health/connectivity
curl https://YOUR_URL.onrender.com/api/users/test

# Test user registration
curl -X POST https://YOUR_URL.onrender.com/api/users/register `
  -H "Content-Type: application/json" `
  -d '{"username":"alice","password":"password123","email":"alice@example.com","role":"RIDER"}'
```

## Next Steps

After backend is deployed:

1. ✅ Backend is live on Render
2. 🔄 Update frontend API URL
3. 🔄 Test frontend with deployed backend
4. 🔄 Deploy frontend to Vercel/Netlify

---

## Troubleshooting

### Build Failed
- Check Render logs for errors
- Verify Java 17 is specified in system.properties
- Ensure pom.xml is correct

### Service Won't Start
- Check start command is exactly as shown above
- Verify webapp-runner.jar was downloaded
- Check logs for port binding errors

### Database Connection Error
- Verify DATABASE_URL is correct (no typos)
- Check database is running on Render
- Ensure database allows connections from Render

### 502 Bad Gateway
- Wait 1-2 minutes for service to fully start
- Check Render logs for startup errors
- Verify PORT environment variable is used

---

**Ready to deploy? Start with Step 1! 🚀**
