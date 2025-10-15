# Test Driver Availability Management
Write-Host "===== Testing Driver Availability Management =====" -ForegroundColor Cyan

# First, create a driver user
Write-Host "`nStep 1: Creating a driver user..." -ForegroundColor Green
$driverBody = @{
    name = "Jane Driver"
    email = "jane.driver@example.com"
    password = "password123"
    role = "DRIVER"
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/users" `
        -Method POST `
        -ContentType "application/json" `
        -Body $driverBody

    $driver = $response.Content | ConvertFrom-Json
    $driverId = $driver.id
    Write-Host "Driver created with ID: $driverId" -ForegroundColor Yellow
    $driver | ConvertTo-Json -Depth 10
} catch {
    Write-Host "Error creating driver: $_" -ForegroundColor Red
    exit
}

Write-Host "`n-------------------`n"

# Set driver availability
Write-Host "Step 2: Setting driver availability schedule..." -ForegroundColor Green
$availabilityBody = @{
    schedules = @(
        @{
            dayOfWeek = "MONDAY"
            startTime = "09:00:00"
            endTime = "17:00:00"
        },
        @{
            dayOfWeek = "WEDNESDAY"
            startTime = "09:00:00"
            endTime = "17:00:00"
        },
        @{
            dayOfWeek = "FRIDAY"
            startTime = "10:00:00"
            endTime = "18:00:00"
        }
    )
} | ConvertTo-Json -Depth 10

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/drivers/availability/$driverId" `
        -Method POST `
        -ContentType "application/json" `
        -Body $availabilityBody

    Write-Host "Availability set successfully!" -ForegroundColor Yellow
    $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
} catch {
    Write-Host "Error setting availability: $_" -ForegroundColor Red
}

Write-Host "`n-------------------`n"

# Get driver availability
Write-Host "Step 3: Getting driver availability schedule..." -ForegroundColor Green
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/drivers/availability/$driverId" `
        -Method GET

    Write-Host "Driver Availability Schedule:" -ForegroundColor Yellow
    $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
} catch {
    Write-Host "Error getting availability: $_" -ForegroundColor Red
}

Write-Host "`n-------------------`n"

# Update driver availability
Write-Host "Step 4: Updating driver availability (add Tuesday)..." -ForegroundColor Green
$updatedAvailabilityBody = @{
    schedules = @(
        @{
            dayOfWeek = "MONDAY"
            startTime = "09:00:00"
            endTime = "17:00:00"
        },
        @{
            dayOfWeek = "TUESDAY"
            startTime = "08:00:00"
            endTime = "16:00:00"
        },
        @{
            dayOfWeek = "WEDNESDAY"
            startTime = "09:00:00"
            endTime = "17:00:00"
        },
        @{
            dayOfWeek = "FRIDAY"
            startTime = "10:00:00"
            endTime = "18:00:00"
        }
    )
} | ConvertTo-Json -Depth 10

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/drivers/availability/$driverId" `
        -Method POST `
        -ContentType "application/json" `
        -Body $updatedAvailabilityBody

    Write-Host "Availability updated successfully!" -ForegroundColor Yellow
    $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
} catch {
    Write-Host "Error updating availability: $_" -ForegroundColor Red
}

Write-Host "`n-------------------`n"

# Get updated availability
Write-Host "Step 5: Getting updated driver availability..." -ForegroundColor Green
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/drivers/availability/$driverId" `
        -Method GET

    Write-Host "Updated Driver Availability Schedule:" -ForegroundColor Yellow
    $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
} catch {
    Write-Host "Error getting availability: $_" -ForegroundColor Red
}

Write-Host "`n-------------------`n"

# Clear driver availability
Write-Host "Step 6: Clearing driver availability..." -ForegroundColor Green
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/drivers/availability/$driverId" `
        -Method DELETE

    Write-Host "Availability cleared successfully!" -ForegroundColor Yellow
    $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
} catch {
    Write-Host "Error clearing availability: $_" -ForegroundColor Red
}

Write-Host "`n-------------------`n"

# Verify availability is cleared
Write-Host "Step 7: Verifying availability is cleared..." -ForegroundColor Green
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/drivers/availability/$driverId" `
        -Method GET

    Write-Host "Driver Availability (should be empty):" -ForegroundColor Yellow
    $availability = $response.Content | ConvertFrom-Json
    if ($availability.Count -eq 0) {
        Write-Host "✓ Availability successfully cleared!" -ForegroundColor Green
    }
    $availability | ConvertTo-Json -Depth 10
} catch {
    Write-Host "Error getting availability: $_" -ForegroundColor Red
}

Write-Host "`n===== Driver Availability Tests Completed =====" -ForegroundColor Cyan
