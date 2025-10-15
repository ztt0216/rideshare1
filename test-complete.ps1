# Complete test script for Ride Management System

Write-Host "=== Ride Management System - Complete Test ===" -ForegroundColor Green

# 1. Initialize database
Write-Host "`n1. Initializing database..." -ForegroundColor Yellow
C:\apache-maven-3.9.11\bin\mvn -f d:\learning\2025sm2\SWEN90007\rideshare1\pom.xml exec:java -Dexec.mainClass="com.rideshare.util.DatabaseInitializer"

# 2. Compile project
Write-Host "`n2. Compiling project..." -ForegroundColor Yellow
C:\apache-maven-3.9.11\bin\mvn -f d:\learning\2025sm2\SWEN90007\rideshare1\pom.xml clean compile

# 3. Stop any running Tomcat
Write-Host "`n3. Stopping any running Tomcat..." -ForegroundColor Yellow
taskkill /F /IM java.exe 2>$null
Start-Sleep -Seconds 2

# 4. Start Tomcat server in background
Write-Host "`n4. Starting Tomcat server..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-Command", "C:\apache-maven-3.9.11\bin\mvn -f d:\learning\2025sm2\SWEN90007\rideshare1\pom.xml tomcat7:run" -WindowStyle Minimized

Write-Host "Waiting for server to start (15 seconds)..." -ForegroundColor Cyan
Start-Sleep -Seconds 15

# 5. Create test users
Write-Host "`n5. Creating test users..." -ForegroundColor Yellow

# Create rider with initial wallet balance
$riderBody = @{
    name = "Alice Rider"
    email = "alice@example.com"
    password = "password123"
    role = "RIDER"
} | ConvertTo-Json

try {
    $rider = Invoke-RestMethod -Uri "http://localhost:8080/api/users" -Method Post -Body $riderBody -ContentType "application/json"
    Write-Host "Rider created: ID $($rider.id)" -ForegroundColor Green
    
    # Add money to rider wallet
    $walletBody = @{
        amount = 500.00
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "http://localhost:8080/api/users/$($rider.id)/wallet" -Method Post -Body $walletBody -ContentType "application/json"
    Write-Host "Added $500 to rider wallet" -ForegroundColor Green
} catch {
    Write-Host "Error creating rider: $_" -ForegroundColor Red
}

# Create driver
$driverBody = @{
    name = "Bob Driver"
    email = "bob@example.com"
    password = "password123"
    role = "DRIVER"
} | ConvertTo-Json

try {
    $driver = Invoke-RestMethod -Uri "http://localhost:8080/api/users" -Method Post -Body $driverBody -ContentType "application/json"
    Write-Host "Driver created: ID $($driver.id)" -ForegroundColor Green
} catch {
    Write-Host "Error creating driver: $_" -ForegroundColor Red
}

# 6. Run ride tests
Write-Host "`n6. Running ride management tests..." -ForegroundColor Yellow
Start-Sleep -Seconds 2
& "d:\learning\2025sm2\SWEN90007\rideshare1\test-rides.ps1"

Write-Host "`n=== Complete Test Finished ===" -ForegroundColor Green
Write-Host "Server is still running. Press Ctrl+C to stop." -ForegroundColor Cyan
