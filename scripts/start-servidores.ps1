<#
.SYNOPSIS
    Inicia os servidores v1 (porta 9001) e v2 (porta 9002), cada um em sua janela.

.DESCRIPTION
    Empacota o servidor se o jar ainda não existir. Logs em logs/server-v1.log e logs/server-v2.log.
    Para encerrar: scripts/stop-servidores.ps1 (ou feche as janelas).

.PARAMETER Ambiente
    v1, v2 ou ambos (padrão).
#>
param(
    [ValidateSet('v1', 'v2', 'ambos')]
    [string]$Ambiente = 'ambos'
)

$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $raiz 'ccbank-server\target\ccbank-server.jar'

if (-not (Test-Path $jar)) {
    Write-Host 'Empacotando o servidor...'
    Push-Location $raiz
    try {
        & mvn -B -q -DskipTests package -pl ccbank-server -am
        if ($LASTEXITCODE -ne 0) { throw 'Falha ao empacotar o servidor.' }
    }
    finally {
        Pop-Location
    }
}

$ambientes = if ($Ambiente -eq 'ambos') { @('v1', 'v2') } else { @($Ambiente) }
foreach ($amb in $ambientes) {
    # chcp 65001: console em UTF-8 para acentos e setas do log
    # || pause: se o servidor não subir, a janela fica aberta mostrando o motivo
    $comando = "title CC_Bank servidor $amb & chcp 65001 >nul & java -cp `"$jar`" br.com.ccbank.servidor.ServidorMain --env=$amb || pause"
    Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', $comando -WorkingDirectory $raiz
    Write-Host "Servidor $amb iniciado em nova janela."
}
