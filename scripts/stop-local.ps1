$ports = 8080,8081,8082,8083,8084,8085,8086,8087,8090,8091,5173
$pids = foreach ($port in $ports) {
  Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess
}

$pids | Sort-Object -Unique | Where-Object { $_ -gt 0 } | ForEach-Object {
  Stop-Process -Id $_ -Force
  [pscustomobject]@{ StoppedPid = $_ }
}
