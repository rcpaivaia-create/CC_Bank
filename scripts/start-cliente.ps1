<#
.SYNOPSIS
    Abre o cliente CC_Bank (metade esquerda → v1, metade direita → v2).

.DESCRIPTION
    Empacota o cliente se o jar ainda não existir. Logs em logs/client-esquerdo.log e
    logs/client-direito.log. Os servidores devem estar no ar (scripts/start-servidores.ps1).
#>
$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $raiz 'ccbank-client\target\ccbank-client.jar'

if (-not (Test-Path $jar)) {
    Write-Host 'Empacotando o cliente...'
    Push-Location $raiz
    try {
        & mvn -B -q -DskipTests package -pl ccbank-client -am
        if ($LASTEXITCODE -ne 0) { throw 'Falha ao empacotar o cliente.' }
    }
    finally {
        Pop-Location
    }
}

Start-Process -FilePath 'javaw' -ArgumentList '-jar', "`"$jar`"" -WorkingDirectory $raiz
Write-Host 'Cliente iniciado.'
