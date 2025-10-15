# Test User Registration
Write-Host "Testing User Registration..." -ForegroundColor Green
$registerBody = @{
    name = "John Doe"
    email = "john@example.com"
    password = "password123"
    role = "RIDER"
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/users" `
        -Method POST `
        -ContentType "application/json" `
        -Body $registerBody

    Write-Host "Registration Response:" -ForegroundColor Yellow
    $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}

Write-Host "`n-------------------`n"

# Test User Login
Write-Host "Testing User Login..." -ForegroundColor Green
$loginBody = @{
    email = "john@example.com"
    password = "password123"
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/users/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $loginBody

    Write-Host "Login Response:" -ForegroundColor Yellow
    $user = $response.Content | ConvertFrom-Json
    $user | ConvertTo-Json -Depth 10
    
    # Save user ID for next test
    $userId = $user.id
    
    Write-Host "`n-------------------`n"
    
    # Test Get User
    Write-Host "Testing Get User by ID..." -ForegroundColor Green
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/users/$userId" `
        -Method GET
    
    Write-Host "Get User Response:" -ForegroundColor Yellow
    $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
    
    Write-Host "`n-------------------`n"
    
    # Test Wallet Update
    Write-Host "Testing Wallet Update..." -ForegroundColor Green
    $walletBody = @{
        amount = 100.50
    } | ConvertTo-Json
    
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/users/$userId/wallet" `
        -Method POST `
        -ContentType "application/json" `
        -Body $walletBody
    
    Write-Host "Wallet Update Response:" -ForegroundColor Yellow
    $response.Content
    
    Write-Host "`n-------------------`n"
    
    # Test Get User Again (to see updated wallet)
    Write-Host "Testing Get User After Wallet Update..." -ForegroundColor Green
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/users/$userId" `
        -Method GET
    
    Write-Host "Updated User Info:" -ForegroundColor Yellow
    $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
    
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}

Write-Host "`n===== Tests Completed =====" -ForegroundColor Cyan
