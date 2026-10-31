# CC_Bank

Laboratório de segurança de transações bancárias: cliente Swing, servidor TCP com protocolo proprietário de rótulo/valor, PostgreSQL e um interceptador que adultera mensagens. O lado esquerdo (v1) é a linha de base; o lado direito (v2) evolui com medidas de segurança cujo impacto de performance é medido.

- Spec (fonte de verdade): `docs/superpowers/specs/2026-10-31-ccbank-laboratorio-design.md`
- Planos de implementação: `docs/superpowers/plans/`

## Regra central do laboratório

- **O v1 é inseguro de propósito.** Sem criptografia, sem sessão, senhas em texto puro, servidor confia nos campos da mensagem. Não "corrija" nem endureça o v1: isso destrói a linha de base dos ataques e do benchmark.
- O v1 fica congelado ao fim da fase 1; só recebe correção de bug que não altere o comportamento vulnerável.
- Medidas de segurança entram apenas no v2, cada uma com sua própria spec.
- Robustez não é segurança: limites como os 64 KB por mensagem valem para os dois lados.

## Build e testes

- Build completo: `mvn -B clean verify`
- Um módulo: `mvn -B -pl ccbank-protocol test`
- Um teste: `mvn -B -pl ccbank-protocol test -Dtest=CodecMensagemTest`
- Não usar `-q` em scripts que dependem da saída do Maven.

## Ambiente e restrições

- Windows 11, PowerShell. Scripts de operação em `scripts/*.ps1`.
- JDK 11 compilando bytecode Java 8 (`maven.compiler.release=8`): nenhuma API posterior ao Java 8.
- Dependências devem ser compatíveis com Java 8; versões centralizadas no `dependencyManagement` do `pom.xml` raiz.
- Encoding UTF-8 em tudo (o padrão da máquina é Cp1252).
- PostgreSQL 18 local. Credenciais em `config/db.properties` (fora do git; modelo em `config/db.properties.example`). Nunca versionar nem exibir esse arquivo.

## Convenções de código

- Nomes de classes, métodos e variáveis em português (`Mensagem`, `CodecMensagem`, `parseValorOperacao`).
- Pacote base `br.com.ccbank`; groupId `br.com.ccbank`.
- Rótulos e códigos de retorno vêm sempre dos enums `Rotulo` e `CodigoRetorno` de `ccbank-protocol`, nunca de strings soltas.
- Valores monetários como `BigDecimal` com escala 2; no fio, ponto decimal (`150.00`).

## Fluxo de trabalho

- Spec → plano → execução com TDD (teste falhando antes do código), um commit por tarefa.
- Trabalho em branch própria por plano (ex.: `fase1a-protocolo`); merge e push só com confirmação.
- Mensagens de commit em português, no formato `feat(modulo): ...` / `fix(modulo): ...`.
