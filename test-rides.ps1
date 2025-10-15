# Test Ride Management API

Write-Host "=== Testing Ride Management System ===" -ForegroundColor Green

# Base URL
$baseUrl = "http://localhost:8080/api"

# 1. Request a new ride (Rider ID 1)
Write-Host "`n1. Requesting a ride (Metro - postcode 3000)..." -ForegroundColor Yellow
$requestRideBody = @{
    riderId = 1
    pickupLocation = "123 Collins St, Melbourne VIC 3000"
    destination = "456 Bourke St, Melbourne VIC 3000"
} | ConvertTo-Json

try {
    $requestRideResponse = Invoke-RestMethod -Uri "$baseUrl/rides" -Method Post -Body $requestRideBody -ContentType "application/json"
    Write-Host "Success: " -ForegroundColor Green -NoNewline
    Write-Host ($requestRideResponse | ConvertTo-Json)
    $rideId = $requestRideResponse.rideId
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}

# 2. Get available rides (for drivers)
Write-Host "`n2. Getting available rides..." -ForegroundColor Yellow
try {
    $availableRides = Invoke-RestMethod -Uri "$baseUrl/rides" -Method Get
    Write-Host "Success: " -ForegroundColor Green -NoNewline
    Write-Host ($availableRides | ConvertTo-Json -Depth 3)
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}

# 3. Accept ride (Driver ID 2)
Write-Host "`n3. Driver accepting ride..." -ForegroundColor Yellow
$acceptRideBody = @{
    driverId = 2
} | ConvertTo-Json

try {
    $acceptResponse = Invoke-RestMethod -Uri "$baseUrl/rides/$rideId/accept" -Method Post -Body $acceptRideBody -ContentType "application/json"
    Write-Host "Success: " -ForegroundColor Green -NoNewline
    Write-Host ($acceptResponse | ConvertTo-Json)
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}

# 4. Start ride
Write-Host "`n4. Driver starting ride..." -ForegroundColor Yellow
$startRideBody = @{
    driverId = 2
} | ConvertTo-Json

try {
    $startResponse = Invoke-RestMethod -Uri "$baseUrl/rides/$rideId/start" -Method Post -Body $startRideBody -ContentType "application/json"
    Write-Host "Success: " -ForegroundColor Green -NoNewline
    Write-Host ($startResponse | ConvertTo-Json)
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}

# 5. Complete ride (triggers payment)
Write-Host "`n5. Driver completing ride (payment will be processed)..." -ForegroundColor Yellow
$completeRideBody = @{
    driverId = 2
} | ConvertTo-Json

try {
    $completeResponse = Invoke-RestMethod -Uri "$baseUrl/rides/$rideId/complete" -Method Post -Body $completeRideBody -ContentType "application/json"
    Write-Host "Success: " -ForegroundColor Green -NoNewline
    Write-Host ($completeResponse | ConvertTo-Json)
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}

# 6. Get ride details
Write-Host "`n6. Getting ride details..." -ForegroundColor Yellow
try {
    $rideDetails = Invoke-RestMethod -Uri "$baseUrl/rides/$rideId" -Method Get
    Write-Host "Success: " -ForegroundColor Green -NoNewline
    Write-Host ($rideDetails | ConvertTo-Json -Depth 2)
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}

# 7. Get rider history
Write-Host "`n7. Getting rider history (Rider ID 1)..." -ForegroundColor Yellow
try {
    $riderHistory = Invoke-RestMethod -Uri "$baseUrl/rides/rider/1" -Method Get
    Write-Host "Success: " -ForegroundColor Green -NoNewline
    Write-Host ($riderHistory | ConvertTo-Json -Depth 3)
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}

# 8. Get driver history
Write-Host "`n8. Getting driver history (Driver ID 2)..." -ForegroundColor Yellow
try {
    $driverHistory = Invoke-RestMethod -Uri "$baseUrl/rides/driver/2" -Method Get
    Write-Host "Success: " -ForegroundColor Green -NoNewline
    Write-Host ($driverHistory | ConvertTo-Json -Depth 3)
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}

# 9. Check user wallets after payment
Write-Host "`n9. Checking user wallets after payment..." -ForegroundColor Yellow
Write-Host "Rider (User 1) wallet:" -ForegroundColor Cyan
try {
    $rider = Invoke-RestMethod -Uri "$baseUrl/users/1" -Method Get
    Write-Host "Balance: $($rider.walletBalance)" -ForegroundColor Green
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}

Write-Host "Driver (User 2) wallet:" -ForegroundColor Cyan
try {
    $driver = Invoke-RestMethod -Uri "$baseUrl/users/2" -Method Get
    Write-Host "Balance: $($driver.walletBalance)" -ForegroundColor Green
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}

# 10. Test fare calculation with different postcodes
Write-Host "`n10. Testing fare calculation for different zones..." -ForegroundColor Yellow

Write-Host "  - Airport (3045): $60" -ForegroundColor Cyan
$airportRideBody = @{
    riderId = 1
    pickupLocation = "123 Collins St, Melbourne VIC 3000"
    destination = "Melbourne Airport, VIC 3045"
} | ConvertTo-Json

try {
    $airportRide = Invoke-RestMethod -Uri "$baseUrl/rides" -Method Post -Body $airportRideBody -ContentType "application/json"
    Write-Host "    Airport ride created: ID $($airportRide.rideId)" -ForegroundColor Green
} catch {
    Write-Host "    Error: $_" -ForegroundColor Red
}

Write-Host "  - Regional (3500): $220" -ForegroundColor Cyan
$regionalRideBody = @{
    riderId = 1
    pickupLocation = "123 Collins St, Melbourne VIC 3000"
    destination = "456 Main St, Bendigo VIC 3500"
} | ConvertTo-Json

try {
    $regionalRide = Invoke-RestMethod -Uri "$baseUrl/rides" -Method Post -Body $regionalRideBody -ContentType "application/json"
    Write-Host "    Regional ride created: ID $($regionalRide.rideId)" -ForegroundColor Green
} catch {
    Write-Host "    Error: $_" -ForegroundColor Red
}

Write-Host "`n=== Ride Management Tests Complete ===" -ForegroundColor Green
