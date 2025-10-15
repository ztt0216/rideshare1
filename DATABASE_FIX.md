# Database Connection Fix

## Issue
Registration was failing with 400 error. Backend logs showed:
```
WARNING: JDBC URL port: -1 not valid (1:65535)
```

## Root Cause
The `DatabaseConfig.java` was parsing the `DATABASE_URL` environment variable incorrectly:
1. When the URL didn't explicitly specify a port, `dbUri.getPort()` returned `-1`
2. PostgreSQL JDBC driver rejected the invalid port number
3. Database connection failed, causing registration to fail

## Solution
Updated `DatabaseConfig.java` in the `getConnectionFromDatabaseUrl()` method:

### Changes Made:
1. **Port handling**: Check if port is -1, use default PostgreSQL port 5432
   ```java
   int port = dbUri.getPort();
   if (port == -1) {
       port = 5432;
   }
   ```

2. **Database path cleaning**: Remove leading slash from database name
   ```java
   String database = dbUri.getPath();
   if (database.startsWith("/")) {
       database = database.substring(1);
   }
   ```

3. **SSL mode**: Added `?sslmode=require` for Render PostgreSQL connection
   ```java
   String jdbcUrl = "jdbc:postgresql://" + dbUri.getHost() + ":" + port + "/" + database + "?sslmode=require";
   ```

4. **Debug logging**: Added console output to verify connection parameters
   ```java
   System.out.println("Connecting to database: " + dbUri.getHost() + ":" + port + "/" + database);
   ```

## Deployment
- Committed fix: `e747998`
- Pushed to GitHub: `main` branch
- Render will automatically redeploy

## Verification Steps
After Render finishes deployment (wait for "Live" status):

1. Check Render logs - should see:
   ```
   Connecting to database: dpg-d3ne6rjipnbc73b1a8a0-a.oregon-postgres.render.com:5432/rideshare1
   ```

2. Test registration from frontend:
   - Open http://localhost:3000
   - Go to Register page
   - Fill in: Name, Email, Password
   - Submit
   - Should succeed without 400 error

3. Verify in browser console (F12):
   ```
   Sending registration data: {name: "...", email: "...", password: "...", role: "RIDER"}
   Registration response: {data: {...}}
   ```

## Related Files
- `src/main/java/com/rideshare/config/DatabaseConfig.java` - Fixed database connection parsing
- `Dockerfile` - Multi-stage build for deployment
- `frontend-new/src/components/Register.jsx` - Frontend registration form with debug logging

## Environment Variables (Render)
Make sure these are set in Render dashboard:
- `DATABASE_URL` - Automatically provided by Render PostgreSQL add-on
- Format: `postgresql://username:password@host:port/database`

## PostgreSQL Connection Details
- **Default Port**: 5432
- **SSL Mode**: Required for Render PostgreSQL
- **Connection String**: `jdbc:postgresql://host:port/database?sslmode=require`
