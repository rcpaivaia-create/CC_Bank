<#
.SYNOPSIS
    Abre a tela do interceptador CC_Bank (aba Esquerdo 8001->9001, aba Direito 8002->9002).

.DESCRIPTION
    Empacota o interceptador se o jar da tela ainda nao existir. Logs em
    logs/interceptor-esquerdo.log e logs/interceptor-direito.log. Os servidores devem estar
    no ar (scripts/start-servidores.ps1) para o trafego fluir.
#>
$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $raiz 'ccbank-interceptor\target\ccbank-interceptor-tela.jar'

if (-not (Test-Path $jar)) {
    Write-Host 'Empacotando o interceptador...'
    Push-Location $raiz
    try {
        & mvn -B -q -DskipTests package -pl ccbank-interceptor -am
        if ($LASTEXITCODE -ne 0) { throw 'Falha ao empacotar o interceptador.' }
    }
    finally {
        Pop-Location
    }
}

Start-Process -FilePath 'javaw' -ArgumentList '-jar', "`"$jar`"" -WorkingDirectory $raiz
Write-Host 'Interceptador iniciado.'
