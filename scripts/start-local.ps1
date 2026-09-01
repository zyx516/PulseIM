param(
  [string]$MysqlUser = "root",
  [string]$MysqlPassword = "pulseim-dev",
  [string]$RabbitHost = "localhost",
  [string]$RabbitUsername = "guest",
  [string]$RabbitPassword = "guest",
  [string]$RedisHost = "localhost",
  [string]$ImNodeId = "im-gateway-local-dlx",
  [string]$ImPort = "8090"
)

$workspace = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $workspace "logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

$env:PULSEIM_MYSQL_USER = $MysqlUser
$env:PULSEIM_MYSQL_PASSWORD = $MysqlPassword
$env:RABBITMQ_HOST = $RabbitHost
$env:RABBITMQ_USERNAME = $RabbitUsername
$env:RABBITMQ_PASSWORD = $RabbitPassword
$env:REDIS_HOST = $RedisHost
$env:IM_NODE_ID = $ImNodeId
$env:IM_PORT = $ImPort

$services = @(
  @{Name="auth-service"; Jar="auth-service\target\auth-service-0.1.0-SNAPSHOT.jar"},
  @{Name="user-service"; Jar="user-service\target\user-service-0.1.0-SNAPSHOT.jar"},
  @{Name="social-service"; Jar="social-service\target\social-service-0.1.0-SNAPSHOT.jar"},
  @{Name="conversation-service"; Jar="conversation-service\target\conversation-service-0.1.0-SNAPSHOT.jar"},
  @{Name="message-service"; Jar="message-service\target\message-service-0.1.0-SNAPSHOT.jar"},
  @{Name="media-service"; Jar="media-service\target\media-service-0.1.0-SNAPSHOT.jar"},
  @{Name="moderation-service"; Jar="moderation-service\target\moderation-service-0.1.0-SNAPSHOT.jar"},
  @{Name="api-gateway"; Jar="api-gateway\target\api-gateway-0.1.0-SNAPSHOT.jar"},
  @{Name="im-gateway"; Jar="im-gateway\target\im-gateway-0.1.0-SNAPSHOT.jar"}
)

foreach ($service in $services) {
  $jarPath = Join-Path $workspace $service.Jar
  $outPath = Join-Path $logDir ($service.Name + ".out.log")
  $errPath = Join-Path $logDir ($service.Name + ".err.log")
  $process = Start-Process -FilePath "java" -ArgumentList @("-jar", $jarPath) -WorkingDirectory $workspace -RedirectStandardOutput $outPath -RedirectStandardError $errPath -WindowStyle Hidden -PassThru
  [pscustomobject]@{ Service = $service.Name; Pid = $process.Id; Log = $outPath }
}
