<#
.SYNOPSIS
    Cria os bancos do CC_Bank (ccbank_v1, ccbank_v2 e os de teste) e aplica os scripts de db/.

.DESCRIPTION
    Sem parâmetros, só cria os bancos que ainda não existem; bancos existentes não são alterados.
    Credenciais em config/db.properties.

.PARAMETER Recriar
    Apaga e recria o schema de todos os bancos (os dados atuais são perdidos).

.PARAMETER SomenteTeste
    Limita a operação aos bancos de teste (ccbank_v1_teste e ccbank_v2_teste).
#>
param(
    [switch]$Recriar,
    [switch]$SomenteTeste
)

$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $raiz 'ccbank-server\target\ccbank-server.jar'

Push-Location $raiz
try {
    if (-not (Test-Path $jar)) {
        Write-Host 'Empacotando o servidor...'
        & mvn -B -q -DskipTests package -pl ccbank-server -am
        if ($LASTEXITCODE -ne 0) { throw 'Falha ao empacotar o servidor.' }
    }

    $argumentos = @()
    if ($Recriar) { $argumentos += '--recriar' }
    if ($SomenteTeste) { $argumentos += '--somente-teste' }

    [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
    & java '-Dsun.stdout.encoding=UTF-8' '-Dsun.stderr.encoding=UTF-8' -cp $jar br.com.ccbank.servidor.dados.SetupBanco @argumentos
    if ($LASTEXITCODE -ne 0) { throw 'Falha no setup do banco.' }
}
finally {
    Pop-Location
}
