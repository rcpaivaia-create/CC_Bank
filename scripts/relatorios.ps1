<#
.SYNOPSIS
    Abre a ferramenta administrativa de relatórios do CC_Bank.

.DESCRIPTION
    Lê o jornal do banco manual (ccbank_v1 ou ccbank_v2) e gera relatórios HTML em relatorios/.
    Empacota o servidor se o jar ainda não existir. Log em logs/admin.log.
#>
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

Start-Process -FilePath 'javaw' -ArgumentList '-cp', "`"$jar`"", 'br.com.ccbank.servidor.relatorio.RelatoriosMain' -WorkingDirectory $raiz
Write-Host 'Ferramenta de relatórios iniciada.'
