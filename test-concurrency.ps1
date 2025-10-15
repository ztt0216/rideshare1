# Concurrency Test - Optimistic Locking Demo
# Tests multiple drivers trying to accept the same ride simultaneously

Write-Host "=== Concurrency Test: Optimistic Locking ===" -ForegroundColor Green
Write-Host "Scenario: Multiple drivers trying to accept the same ride" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080/api"

# 1. Create a test ride
Write-Host "`n1. Creating a test ride..." -ForegroundColor Yellow
$rideBody = @{
    riderId = 1
    pickupLocation = "123 Collins St, Melbourne VIC 3000"
    destination = "Melbourne Airport, VIC 3045"
} | ConvertTo-Json

try {
    $ride = Invoke-RestMethod -Uri "$baseUrl/rides" -Method Post -Body $rideBody -ContentType "application/json"
    $rideId = $ride.rideId
    Write-Host "✅ Ride created: ID $rideId (Fare: $60 - Airport)" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to create ride: $_" -ForegroundColor Red
    exit
}

# 2. Verify ride is in REQUESTED status
Write-Host "`n2. Verifying ride status..." -ForegroundColor Yellow
try {
    $rideDetails = Invoke-RestMethod -Uri "$baseUrl/rides/$rideId" -Method Get
    Write-Host "✅ Ride Status: $($rideDetails.status)" -ForegroundColor Green
    Write-Host "   Version: $($rideDetails.version)" -ForegroundColor Cyan
} catch {
    Write-Host "❌ Failed to get ride details" -ForegroundColor Red
}

# 3. Simulate multiple drivers accepting simultaneously
Write-Host "`n3. Simulating concurrent acceptance by 3 drivers..." -ForegroundColor Yellow
Write-Host "   (Using parallel PowerShell jobs)" -ForegroundColor Cyan

$acceptRideScript = {
    param($rideId, $driverId, $baseUrl)
    
    $body = @{
        driverId = $driverId
    } | ConvertTo-Json
    
    try {
        $result = Invoke-RestMethod -Uri "$baseUrl/rides/$rideId/accept" -Method Post -Body $body -ContentType "application/json"
        return @{
            driverId = $driverId
            success = $true
            message = $result.message
        }
    } catch {
        return @{
            driverId = $driverId
            success = $false
            error = $_.Exception.Message
        }
    }
}

# Start 3 parallel jobs (drivers 2, 3, 4 trying to accept)
$jobs = @()
Write-Host "`n   Starting acceptance attempts..." -ForegroundColor Cyan

# Driver 2
$jobs += Start-Job -ScriptBlock $acceptRideScript -ArgumentList $rideId, 2, $baseUrl
Write-Host "   • Driver 2: Attempting to accept..." -ForegroundColor Gray

# Driver 3
$jobs += Start-Job -ScriptBlock $acceptRideScript -ArgumentList $rideId, 3, $baseUrl
Write-Host "   • Driver 3: Attempting to accept..." -ForegroundColor Gray

# Driver 4
$jobs += Start-Job -ScriptBlock $acceptRideScript -ArgumentList $rideId, 4, $baseUrl
Write-Host "   • Driver 4: Attempting to accept..." -ForegroundColor Gray

# Wait for all jobs to complete
Write-Host "`n   Waiting for results..." -ForegroundColor Cyan
$results = $jobs | Wait-Job | Receive-Job
$jobs | Remove-Job

# 4. Display results
Write-Host "`n4. Results:" -ForegroundColor Yellow

$successCount = 0
foreach ($result in $results) {
    if ($result.success) {
        $successCount++
        Write-Host "   ✅ Driver $($result.driverId): SUCCESS - $($result.message)" -ForegroundColor Green
    } else {
        Write-Host "   ❌ Driver $($result.driverId): FAILED - $($result.error)" -ForegroundColor Red
    }
}

# 5. Verify final state
Write-Host "`n5. Verifying final ride state..." -ForegroundColor Yellow
try {
    $finalRide = Invoke-RestMethod -Uri "$baseUrl/rides/$rideId" -Method Get
    Write-Host "   Status: $($finalRide.status)" -ForegroundColor Cyan
    Write-Host "   Accepted by Driver: $($finalRide.driverId)" -ForegroundColor Cyan
    Write-Host "   Version: $($finalRide.version)" -ForegroundColor Cyan
} catch {
    Write-Host "   ❌ Failed to get final ride state" -ForegroundColor Red
}

# 6. Summary
Write-Host "`n6. Test Summary:" -ForegroundColor Yellow
if ($successCount -eq 1) {
    Write-Host "   ✅ PASS: Optimistic locking working correctly!" -ForegroundColor Green
    Write-Host "   • Only 1 driver succeeded (as expected)" -ForegroundColor Green
    Write-Host "   • Other drivers received 'Optimistic lock failure' error" -ForegroundColor Green
} else {
    Write-Host "   ❌ FAIL: Expected 1 success, got $successCount" -ForegroundColor Red
}

Write-Host "`n=== Concurrency Test Complete ===" -ForegroundColor Green

# Additional information
Write-Host "`n📚 How Optimistic Locking Works:" -ForegroundColor Cyan
Write-Host "   1. Each ride has a 'version' field (starts at 0)" -ForegroundColor Gray
Write-Host "   2. When accepting, SQL checks: WHERE id = ? AND version = ?" -ForegroundColor Gray
Write-Host "   3. If version matches, update succeeds and version increments" -ForegroundColor Gray
Write-Host "   4. If version changed (another driver accepted), update fails" -ForegroundColor Gray
Write-Host "   5. Only the first driver's transaction commits successfully" -ForegroundColor Gray
