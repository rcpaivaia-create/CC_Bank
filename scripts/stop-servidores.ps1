<#
.SYNOPSIS
    Encerra os servidores do CC_Bank (processos java do ServidorMain e as janelas que os abriram).
#>
$alvo = '*br.com.ccbank.servidor.ServidorMain*'
$servidores = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" |
    Where-Object { $_.CommandLine -like $alvo }
$janelas = Get-CimInstance Win32_Process -Filter "Name = 'cmd.exe'" |
    Where-Object { $_.CommandLine -like $alvo }

if (-not $servidores -and -not $janelas) {
    Write-Host 'Nenhum servidor do CC_Bank em execução.'
    return
}
foreach ($p in $servidores) {
    $env = if ($p.CommandLine -match '--env=(\w+)') { $Matches[1] } else { '?' }
    Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
    Write-Host "Servidor $env (PID $($p.ProcessId)) encerrado."
}
foreach ($j in $janelas) {
    Stop-Process -Id $j.ProcessId -Force -ErrorAction SilentlyContinue
}
