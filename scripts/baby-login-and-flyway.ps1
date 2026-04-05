param(
    [string]$BaseUrl = "http://localhost:5603",
    [string]$Username = "admin",
    [string]$Password = "Admin@123"
)

$loginBody = @{
    username = $Username
    password = $Password
} | ConvertTo-Json

Write-Host ""
Write-Host "1. 登录..."
$loginResponse = Invoke-RestMethod `
    -Method Post `
    -Uri "$BaseUrl/auth/login" `
    -ContentType "application/json" `
    -Body $loginBody

if (-not $loginResponse.data -or -not $loginResponse.data.token) {
    throw "登录成功响应里没有拿到 token，请检查接口返回。"
}

$token = $loginResponse.data.token
$headers = @{
    Authorization = "Bearer $token"
}

Write-Host "登录成功。"
Write-Host "token 前 20 位: $($token.Substring(0, [Math]::Min(20, $token.Length)))..."

Write-Host ""
Write-Host "2. 查看当前用户..."
$meResponse = Invoke-RestMethod `
    -Method Get `
    -Uri "$BaseUrl/auth/me" `
    -Headers $headers

$meResponse | ConvertTo-Json -Depth 6

Write-Host ""
Write-Host "3. 查看当前会话..."
$sessionResponse = Invoke-RestMethod `
    -Method Get `
    -Uri "$BaseUrl/auth/session/current" `
    -Headers $headers

$sessionResponse | ConvertTo-Json -Depth 6

Write-Host ""
Write-Host "4. 查看 Flyway 状态..."
$flywayResponse = Invoke-RestMethod `
    -Method Get `
    -Uri "$BaseUrl/actuator/flyway" `
    -Headers $headers

$flywayResponse | ConvertTo-Json -Depth 8

Write-Host ""
Write-Host "完成。"
Write-Host "如果这里能正常看到用户信息和 Flyway 数据，说明登录、鉴权、Actuator 访问链路基本通了。"
