# Fase 1c (parte 2) — Validação da senha em transação separada — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Separar a conferência da senha do cliente numa transação própria (`validar_senha`), executada pelo cliente antes de saldo, saque, depósito, transferência e extrato, que deixam de levar a senha.

**Architecture:** No servidor, nova `OperacaoValidarSenha`; as cinco operações passam a só localizar a conta (sem senha). No cliente, `ClienteBanco` ganha `validarSenha` e `executarComValidacao`, que envia a validação e, só com `000`, a operação; as telas exibem as duas trocas. O servidor v1 **não** liga a validação à operação (vulnerabilidade de propósito).

**Tech Stack:** Java (bytecode 8, JDK 11), Swing, Maven 3.9, JUnit 5.11.3, PostgreSQL 18.

**Spec:** `docs/superpowers/specs/2026-10-31-ccbank-laboratorio-design.md` (seções 4.4, 4.5, 7.3, 8.1, 9.2 — revisão de 2026-10-31)

## Global Constraints

- Tudo de `CLAUDE.md` vale aqui.
- Transação `validar_senha`: obrigatórios 00002, 00004–00006, 00010, 00011, 00012. Resposta `000` (com 00093) / `002` / `004` / `006`, sempre com 00094. Mensagem de sucesso (00091): `Senha validada`. Tipo no jornal: `VALIDAR_SENHA`. Não altera saldo nem gera lançamento. Intercredis calculado como nas demais.
- Saldo, saque, depósito, transferência e extrato: 00012 deixa de ser obrigatório e, se vier, é ignorado.
- Nova ordem das validações nas operações: campos obrigatórios (006) → valor (005) → conta igual (007) → conta de origem existe (004) → conta destino existe (004) → saldo (003).
- O servidor v1 aceita qualquer operação sem validação prévia.
- Cliente: a operação só é enviada se a validação devolver `000`.

## Review Focus

1. **Senha errada no cliente:** a operação não é enviada — o jornal tem só a linha `VALIDAR_SENHA` com `002`, nenhuma linha do tipo da operação. → teste em Task 2.
2. **Validação ok mas operação recusada** (ex.: saldo insuficiente): o cliente mostra o erro da operação, não o "Senha validada". → teste em Task 2.
3. **Senha enviada junto na operação** (cliente antigo ou adulteração): ignorada pelo servidor, mesmo se errada. → teste em Task 1.
4. **Falha de rede entre as duas etapas:** a exceção chega à tela e nada é efetivado além do que o servidor respondeu. → coberto pelo tratamento existente de `IOException` em `PainelLado.executar`; teste em Task 2 (validação ok, servidor parado antes da operação → `IOException`).
5. **Conta inexistente:** a validação já responde `004` e a operação não é enviada. → teste em Task 2.

---

### Task 1: Servidor — `validar_senha` e operações sem senha

**Files:**
- Create: `ccbank-server/src/main/java/br/com/ccbank/servidor/negocio/OperacaoValidarSenha.java`
- Modify: `ccbank-server/src/main/java/br/com/ccbank/servidor/negocio/Regras.java` (`ORIGEM` sem `SENHA_CONTA`; `autenticarConta` vira `localizarConta` — só 004 e Intercredis; nova `conferirSenha(Conta, Mensagem)` → `002`)
- Modify: `OperacaoSaldo`, `OperacaoSaque`, `OperacaoDeposito`, `OperacaoTransferencia`, `OperacaoExtrato` (usam `localizarConta`)
- Modify: `ProcessadorTransacoes.java` (registra `validar_senha`)
- Modify: `ccbank-tests/src/test/java/br/com/ccbank/testes/Requisicoes.java` — `conta(m, coop, conta)` sem senha; `saldo(coop, conta)`, `saque(coop, conta, valor)`, `deposito(coop, conta, valor)`, `transferencia(coop, conta, coopDestino, contaDestino, valor)`, `extrato(coop, conta, inicio, fim)`; novo `validarSenha(coop, conta, senha)`
- Modify: todos os ITs que usam `Requisicoes` (remover o argumento de senha; testes de senha errada em operações migram para `validar_senha`)
- Create: `ccbank-tests/src/test/java/br/com/ccbank/testes/ValidarSenhaIT.java`

**Interfaces:**
- Produces: `Regras.localizarConta(Connection, Mensagem, RegistroJornal) : Conta`; `Regras.conferirSenha(Conta, Mensagem) throws Recusa`; transação `validar_senha`; assinaturas novas de `Requisicoes` acima.

- [ ] **Step 1: Escrever os testes que falham** (`ValidarSenhaIT`, parametrizado por `Ambiente`)

```java
void senhaCorreta        → validarSenha("0001","100001","123456"): "000", 00093="Maria Souza", 00094 presente;
                            jornal 1 linha VALIDAR_SENHA '000' conta_origem_id preenchido; 0 lançamentos; saldo inalterado
void senhaErrada         → "002", sem 00093; jornal VALIDAR_SENHA '002'
void contaInexistente    → ("0001","999999","123456"): "004"
void semSenha            → cabecalho("validar_senha") + 00010/00011 sem 00012: "006"
void intercredisNaValidacao → validarSenha("0002","200001","123456"): jornal intercredis=true
void operacaoSemValidacaoEhAceitaNoV1 → saque("0001","100001","10.00") sem validar antes: "000"; saldo 990.00
void senhaNaOperacaoEhIgnorada → saque com 00012="000000" acrescentado: "000"
```

- [ ] **Step 2: Rodar e confirmar a falha**

Run: `mvn -B clean verify -Pintegracao -pl ccbank-tests -am -Dit.test=ValidarSenhaIT`
Expected: FAIL de compilação (`Requisicoes.validarSenha` não existe).

- [ ] **Step 3: Implementar e ajustar `Requisicoes` e os ITs existentes**

Nos ITs: remover o argumento `"123456"` das chamadas de operação. Testes que esperavam `002` numa operação (`LoginSaldoIT.saldoSenhaErrada`, `LoginSaldoIT.recusaTambemVaiParaOJornal`, `SaqueDepositoIT.depositoSenhaErrada`, `TransferenciaIT.senhaErrada`) são removidos — a cobertura passa a `ValidarSenhaIT`. `campoObrigatorioAusente` (saldo) passa a omitir `00011`.

- [ ] **Step 4: Rodar e confirmar que passa**

Run: `mvn -B clean verify -Pintegracao`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add ccbank-server ccbank-tests
git commit -m "feat(servidor): validação de senha em transação separada; operações sem senha"
```

---

### Task 2: Cliente — API com validação em duas etapas

**Files:**
- Modify: `ccbank-client/src/main/java/br/com/ccbank/cliente/ClienteBanco.java`
- Create: `ccbank-client/src/main/java/br/com/ccbank/cliente/OperacaoCliente.java` (`@FunctionalInterface Resposta executar() throws IOException`)
- Create: `ccbank-client/src/main/java/br/com/ccbank/cliente/ResultadoValidado.java`
- Modify: `ccbank-tests/src/test/java/br/com/ccbank/testes/ClienteBancoIT.java`

**Interfaces:**
- Produces:
  - `ClienteBanco.validarSenha(coop, conta, senha) : Resposta`
  - Operações sem senha: `saldo(coop, conta)`, `saque(coop, conta, valorDigitado)`, `deposito(coop, conta, valorDigitado)`, `transferencia(coop, conta, coopDestino, contaDestino, valorDigitado)`, `extrato(coop, conta, inicioDigitado, fimDigitado)`.
  - `ResultadoValidado executarComValidacao(String coop, String conta, String senha, OperacaoCliente operacao) throws IOException` — envia a validação; se não for `000`, devolve sem executar a operação.
  - `ResultadoValidado`: `Resposta validacao()`, `Resposta operacao()` (`null` se a validação falhou), `Resposta resultadoFinal()` (a operação, ou a validação se ela falhou), `List<Resposta> respostas()` (1 ou 2, na ordem), `boolean sucesso()` (= `resultadoFinal().sucesso()`).

- [ ] **Step 1: Escrever os testes que falham** (`ClienteBancoIT`; ajustar os existentes às novas assinaturas)

```java
void validacaoEOperacao → executarComValidacao("0001","100001","123456", () -> cliente.saque("0001","100001","10,00")):
    sucesso; respostas().size()=2; operacao().requisicao() sem 00012; jornal: 1 VALIDAR_SENHA e 1 SAQUE
void senhaErradaNaoEnviaOperacao → senha "000000": !sucesso; resultadoFinal().codigo()="002"; operacao()==null; 0 linhas SAQUE no jornal
void contaInexistenteNaoEnviaOperacao → ("0001","999999"): resultadoFinal().codigo()="004"; operacao()==null
void operacaoRecusadaAposValidacao → saque "5.000,00" em 0001/100002: validacao().sucesso(); resultadoFinal().codigo()="003"
void quedaEntreAsEtapas → operação que para o servidor antes de enviar o saque: lança IOException
```

- [ ] **Step 2: Rodar e confirmar a falha**

Run: `mvn -B clean verify -Pintegracao -pl ccbank-tests -am -Dit.test=ClienteBancoIT`
Expected: FAIL de compilação.

- [ ] **Step 3: Implementar `OperacaoCliente`, `ResultadoValidado` e os métodos de `ClienteBanco`**

- [ ] **Step 4: Rodar e confirmar que passa**

Run: `mvn -B clean verify -Pintegracao`
Expected: BUILD SUCCESS (as telas ainda não compilam com as assinaturas novas → ajustar a chamada mínima em `FormularioOperacao`/`PainelExtrato` para compilar; o fluxo completo de tela vem na Task 3).

- [ ] **Step 5: Commit**

```bash
git add ccbank-client ccbank-tests
git commit -m "feat(cliente): validação de senha antes de cada operação"
```

---

### Task 3: Telas — duas etapas, tempos e relatório

**Files:**
- Modify: `ccbank-client/src/main/java/br/com/ccbank/cliente/tela/PainelLado.java` (`executar(Callable<List<Resposta>>, Consumer<List<Resposta>>)`; registra todas as trocas no painel de mensagens; login passa `Collections.singletonList`)
- Modify: `FormularioOperacao.java`, `PainelExtrato.java` (usam `executarComValidacao`; mostram o erro da validação quando ela falha; tempo `validação N ms · operação M ms`)
- Create: `ccbank-client/src/main/java/br/com/ccbank/cliente/tela/TextoResultado.java` (formatação testável)
- Modify: `ccbank-server/src/main/java/br/com/ccbank/servidor/relatorio/JanelaRelatorios.java` (combo de operação inclui `VALIDAR_SENHA`)
- Test: `ccbank-client/src/test/java/br/com/ccbank/cliente/tela/TextoResultadoTest.java`

**Interfaces:**
- Produces: `TextoResultado.tempos(ResultadoValidado r) : String` → `validação 12 ms · operação 30 ms`, ou `validação 12 ms` quando a operação não foi enviada; `TextoResultado.situacao(ResultadoValidado r) : String` → `✔ <00091 da operação>` ou `✖ <00091 de quem falhou>`.

- [ ] **Step 1: Escrever os testes que falham** (unidade, `ResultadoValidado` montado com `Resposta`s à mão)

```java
@Test void temposComAsDuasEtapas()   → "validação 12 ms · operação 30 ms"
@Test void temposSemOperacao()       → "validação 12 ms"
@Test void situacaoSucesso()         → "✔ Saque efetuado"
@Test void situacaoFalhaNaValidacao() → "✖ senha da conta inválida"
@Test void situacaoFalhaNaOperacao()  → "✖ saldo insuficiente"
```

- [ ] **Step 2: Rodar e confirmar a falha**

Run: `mvn -B -pl ccbank-client -am test`
Expected: FAIL de compilação.

- [ ] **Step 3: Implementar `TextoResultado` e ajustar as telas e o combo do relatório**

- [ ] **Step 4: Rodar a suíte completa**

Run: `mvn -B clean verify -Pintegracao`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Verificação do executor**

O jar do cliente abre sem erro. A conferência visual entra no roteiro do usuário: saque com senha errada mostra `✖ senha da conta inválida` e o painel Mensagens tem só a troca `validar_senha`.

- [ ] **Step 6: Commit**

```bash
git add ccbank-client ccbank-server
git commit -m "feat(cliente): telas executam validação e operação em duas etapas"
```
