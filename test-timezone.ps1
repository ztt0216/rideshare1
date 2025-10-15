# Test Timezone Configuration
Write-Host "===== Testing Timezone Configuration =====" -ForegroundColor Cyan

Write-Host "`nCurrent System Time (Windows):" -ForegroundColor Green
Get-Date -Format "yyyy-MM-dd HH:mm:ss K"

Write-Host "`nMelbourne Time (Expected):" -ForegroundColor Green
$melbourneTime = [System.TimeZoneInfo]::ConvertTimeBySystemTimeZoneId((Get-Date), 'AUS Eastern Standard Time')
Write-Host $melbourneTime.ToString("yyyy-MM-dd HH:mm:ss")
Write-Host "Day of Week: $($melbourneTime.DayOfWeek)"

Write-Host "`n" -NoNewline
Write-Host "Note: " -ForegroundColor Yellow -NoNewline
Write-Host "The application uses Australia/Melbourne timezone for all time-related operations."

Write-Host "`n===== Timezone Test Completed =====" -ForegroundColor Cyan
