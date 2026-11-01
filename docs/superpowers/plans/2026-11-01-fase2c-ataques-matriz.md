# Fase 2c — Catálogo de ataques, testes de ataque e matriz HTML — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rodar automaticamente os 14 ataques do catálogo contra os dois lados pelo interceptador (e por executores offline/observacional), comparar com as expectativas e emitir uma matriz HTML.

**Architecture:** Harness em `ccbank-tests` que, por `Ambiente`, reseta o banco, sobe `ServidorTcp` + `ServidorProxy` (headless, 2a) + `ClienteTeste` através do proxy, carrega a regra do ataque do catálogo e mede o veredito pela resposta e pelo banco. Ataques sem regra usam `Replay`, um `ListAppender` de log, ou computação offline. Um `AtaquesIT` parametrizado (ataque × lado) compara com `expectativas-ataques.properties` e gera a matriz.

**Tech Stack:** Java 8 (JDK 11, `--release 8`), JUnit 5.11.3, failsafe (perfil `integracao`), `ccbank-protocol`, `ccbank-server`, `ccbank-client`, `ccbank-interceptor`, PostgreSQL real.

**Spec:** `docs/superpowers/specs/2026-11-01-fase2c-ataques-matriz-design.md` (fonte de verdade); spec-mãe seções 7.3 e 9.3.

## Global Constraints

- `maven.compiler.release` = `8`; nenhuma API posterior ao Java 8. `sourceEncoding` = `UTF-8`. Sem dependências novas.
- Todo o código em `ccbank-tests` (`src/test/java`); os testes de ataque são `*IT`, rodando sob `mvn -B clean verify -Pintegracao` (PostgreSQL real).
- O catálogo fica em `regras/ataques-exemplo.json` na **raiz do repositório** (carregável na tela e pelo harness); o harness o localiza subindo a árvore de diretórios a partir de `user.dir`.
- Vereditos: `VULNERAVEL`, `PROTEGIDO`. `expectativas-ataques.properties` começa com tudo `VULNERAVEL` nos dois lados (`v1`, `v2`).
- Lados vêm de `Ambiente` (`V1`→`v1`, `V2`→`v2`); hoje o código é idêntico, então todos os vereditos são `VULNERAVEL`.
- A matriz HTML é autocontida (CSS embutido) e vai para `resultados/matriz-ataques-<AAAA-MM-DD-HHMMSS>.html` (fora do git).
- Rótulos (de `Rotulo`): valor `00030`, conta destino `00021`, cooperativa/pac/terminal do operador `00004`/`00005`/`00006`, operador `00002`, código de retorno `00090`, saldo `00092`, senha do operador `00003`.
- Contas de seed usadas: 0001/100001 (Maria, 1000.00), 0001/100002 (João, 500.00), 0001/100010 (Pedro, 0.00); operadores `rodrigop`/`ana.lima`, senha `senha123`; senha de conta `123456`.

## Review Focus

1. **#6 precisa de saldo insuficiente real**: usar a conta 0001/100010 (saldo 0.00) para o servidor devolver `003`; com saldo suficiente o ataque mede errado. → teste na Task 3.
2. **#3 conta de destino forjada precisa existir**: redirecionar para 0001/100010 (existe), senão o servidor devolve `004` e nada é creditado. → teste na Task 2.
3. **Resolução do catálogo independe do CWD**: o harness acha `regras/ataques-exemplo.json` subindo de `user.dir` (o CWD do failsafe é o diretório do módulo). → teste na Task 1.
4. **#9 captura antes da troca**: o `ListAppender` tem de estar ligado ao logger do lado antes de a operação passar, senão não captura. → teste na Task 4.
5. **Expectativa ausente para um (ataque, lado)**: o `AtaquesIT` falha com mensagem clara, não passa em silêncio nem estoura `NullPointerException`. → teste na Task 6.

---

### Task 1: Catálogo de regras + infra do harness (`Veredito`, `Ataque`, `ContextoAtaque`, `SessaoAtaque`)

**Files:**
- Create: `regras/ataques-exemplo.json` (raiz do repo)
- Create: `ccbank-tests/src/test/java/br/com/ccbank/testes/ataques/Veredito.java`
- Create: `.../ataques/Ataque.java`, `.../ataques/ContextoAtaque.java`, `.../ataques/SessaoAtaque.java`, `.../ataques/CatalogoRegras.java`
- Test: `.../ataques/ContextoAtaqueIT.java`, `.../ataques/CatalogoRegrasTest.java`

**Interfaces:**
- Consumes: `Ambiente`, `ServidorTcp`, `ProcessadorTransacoes` (servidor); `ServidorProxy`, `MediadorRegras`, `MediadorPassagem`, `OuvinteTroca` (interceptador); `MotorRegras`, `Regra`, `CarregadorRegras` (interceptador); `ClienteTeste`, `BancoTeste`, `Requisicoes` (teste); `CodecMensagem`.
- Produces:
  - `enum Veredito { VULNERAVEL, PROTEGIDO }`.
  - `interface Ataque { String id(); String descricao(); Veredito avaliar(ContextoAtaque contexto) throws Exception; }`
  - `final class CatalogoRegras` — `static java.nio.file.Path arquivo()` (sobe de `user.dir` até achar `regras/ataques-exemplo.json`); `static List<Regra> regra(String nome)` (carrega o catálogo com `CarregadorRegras.carregar` e filtra pela regra de `nome`; erro claro se não existir).
  - `final class ContextoAtaque` — `ContextoAtaque(Ambiente ambiente, javax.sql.DataSource ds)`; `Ambiente ambiente()`; `javax.sql.DataSource ds()`; `SessaoAtaque abrir(List<Regra> regras) throws IOException` (regras vazias = passagem).
  - `final class SessaoAtaque implements AutoCloseable` — `ClienteTeste cliente()`; `int portaServidor()`; `List<String> logCapturado()`; `close()` para proxy, servidor e cliente e solta o `ListAppender`.

- [ ] **Step 1: Escrever `regras/ataques-exemplo.json` e os testes que falham**

`ataques-exemplo.json` (regras nomeadas; uma por ataque dirigido por regra):

```json
[
  { "nome": "valor-adulterado", "direcao": "REQUISICAO", "quando": {"00001":"saque"}, "acoes": [{"tipo":"SET","rotulo":"00030","valor":"5.00"}] },
  { "nome": "deposito-inflado", "direcao": "REQUISICAO", "quando": {"00001":"deposito"}, "acoes": [{"tipo":"SET","rotulo":"00030","valor":"10000.00"}] },
  { "nome": "transferencia-desviada", "direcao": "REQUISICAO", "quando": {"00001":"transferencia"}, "acoes": [{"tipo":"SET","rotulo":"00021","valor":"100010"}] },
  { "nome": "origem-falsificada", "direcao": "REQUISICAO", "quando": {"00001":"saque"}, "acoes": [{"tipo":"SET","rotulo":"00004","valor":"9999"},{"tipo":"SET","rotulo":"00005","valor":"999"},{"tipo":"SET","rotulo":"00006","valor":"999"}] },
  { "nome": "operador-falsificado", "direcao": "REQUISICAO", "quando": {"00001":"saque"}, "acoes": [{"tipo":"SET","rotulo":"00002","valor":"ana.lima"}] },
  { "nome": "resposta-falsa", "direcao": "RESPOSTA", "quando": {"00090":"003"}, "acoes": [{"tipo":"SET","rotulo":"00090","valor":"000"}] },
  { "nome": "saldo-exibido-falso", "direcao": "RESPOSTA", "quando": {"00092":"~.+"}, "acoes": [{"tipo":"SET","rotulo":"00092","valor":"9999.99"}] },
  { "nome": "validacao-falsificada", "direcao": "RESPOSTA", "quando": {"00090":"002"}, "acoes": [{"tipo":"SET","rotulo":"00090","valor":"000"}] }
]
```

```java
// CatalogoRegrasTest (mvn test, sem banco)
@Test void achaOArquivoNaRaiz() { assertTrue(java.nio.file.Files.exists(CatalogoRegras.arquivo())); }
@Test void carregaUmaRegraPorNome() {
    List<Regra> r = CatalogoRegras.regra("valor-adulterado");
    assertEquals(1, r.size());
    assertEquals(Sentido.REQUISICAO, r.get(0).sentido());
}
@Test void regraInexistenteFalhaClaro() {
    assertThrows(IllegalArgumentException.class, () -> CatalogoRegras.regra("nao-existe"));
}

// ContextoAtaqueIT (perfil integracao)
@ParameterizedTest @EnumSource(Ambiente.class)
void sessaoEntregaLoginPeloProxy(Ambiente amb) throws Exception {
    ContextoAtaque ctx = new ContextoAtaque(amb, BancoTeste.recriar(amb));
    try (SessaoAtaque s = ctx.abrir(java.util.Collections.emptyList())) {
        assertEquals(Optional.of("000"),
            s.cliente().enviar(Requisicoes.login("rodrigop","senha123")).primeiro(Rotulo.CODIGO_RETORNO));
    }
}
```

- [ ] **Step 2: Rodar e ver falhar** — `mvn -B -pl ccbank-tests -am test -Dtest=CatalogoRegrasTest -Dsurefire.failIfNoSpecifiedTests=false` → FAIL.
- [ ] **Step 3: Implementar** `Veredito`, `Ataque`, `CatalogoRegras`, `ContextoAtaque`, `SessaoAtaque`. `ContextoAtaque.abrir`: cria `ServidorTcp(0, new ProcessadorTransacoes(ds), ambiente.lado())`, `iniciar()`; cria o mediador (`new MediadorRegras(new MotorRegras(regras))` ou `new MediadorPassagem()` se vazio); cria `ServidorProxy(0,"127.0.0.1",portaServidor,mediador,OuvinteTroca.NULO,"ataque")`, `iniciar()`; obtém o `ch.qos.logback.classic.Logger` `ccbank.interceptador.ataque`, **põe o nível em `INFO`** (o root do módulo de teste é `WARN`, então sem isso os eventos `INFO` do proxy não chegam ao appender) e liga um `ch.qos.logback.core.read.ListAppender`; abre `ClienteTeste(proxy.portaLocal())`. `logCapturado()` devolve `event.getFormattedMessage()` de cada evento; `close()` solta o appender e para proxy, servidor e cliente.
- [ ] **Step 4: Rodar e ver passar** — `mvn -B -pl ccbank-tests -am test -Dtest=CatalogoRegrasTest -Dsurefire.failIfNoSpecifiedTests=false` PASS; `mvn -B clean verify -Pintegracao -Dit.test=ContextoAtaqueIT` PASS.
- [ ] **Step 5: Commit** — `feat(ataques): catálogo de regras e infra do harness de ataque`.

---

### Task 2: Ataques de requisição que alteram o banco (#1–#5)

**Files:**
- Create: `.../ataques/ValorAdulterado.java`, `DepositoInflado.java`, `TransferenciaDesviada.java`, `OrigemFalsificada.java`, `OperadorFalsificado.java`
- Test: `.../ataques/AtaquesRequisicaoIT.java`

**Interfaces:**
- Consumes: `Ataque`, `ContextoAtaque`, `SessaoAtaque`, `Veredito`, `CatalogoRegras` (Task 1); `Requisicoes`, `BancoTeste`, `Rotulo`.
- Produces: cada classe implementa `Ataque` com `id()` igual ao nome da regra e `avaliar` que carrega a regra, executa a operação e devolve `VULNERAVEL` se o banco refletir a adulteração.

- [ ] **Step 1: Escrever os testes que falham** (parametrizado por ataque, só o lado V1 para a asserção de VULNERÁVEL)

```java
// AtaquesRequisicaoIT
@Test void valorAdulteradoDebitaValorErrado() throws Exception {
    ContextoAtaque ctx = new ContextoAtaque(Ambiente.V1, BancoTeste.recriar(Ambiente.V1));
    assertEquals(Veredito.VULNERAVEL, new ValorAdulterado().avaliar(ctx));
    // saque pedido de 500.00 em 100001 (1000.00) vira 5.00 -> saldo final 995.00
    assertEquals(new BigDecimal("995.00"), BancoTeste.saldo(ctx.ds(), "0001", "100001"));
}
@Test void depositoInfladoCreditaAMais() throws Exception { /* depósito 10.00 em 100002 vira 10000.00 -> 10500.00 */ }
@Test void transferenciaDesviadaCreditaOutraConta() throws Exception {
    // transfere 100.00 de 100001 para 100002, regra redireciona p/ 100010 -> 100010 recebe 100.00, 100002 intacta
}
@Test void origemFalsificadaGravaOrigemForjada() throws Exception {
    // saque; jornal grava cooperativa_operador=9999 (divergente do terminal cadastrado)
}
@Test void operadorFalsificadoGravaOutroOperador() throws Exception {
    // saque logado como rodrigop chega como ana.lima no jornal
}
```

- [ ] **Step 2: Rodar e ver falhar** → FAIL.
- [ ] **Step 3: Implementar os cinco ataques.** Cada `avaliar`: `try (SessaoAtaque s = contexto.abrir(CatalogoRegras.regra(id())))` faz login, executa a operação (ex.: `s.cliente().enviar(Requisicoes.saque("0001","100001","500.00"))`), e devolve `VULNERAVEL`/`PROTEGIDO` conforme o estado do banco (via `BancoTeste.saldo`/`contar`). Para origem/operador, checa o jornal com `BancoTeste.contar`/`texto`.
- [ ] **Step 4: Rodar e ver passar** — `mvn -B clean verify -Pintegracao -Dit.test=AtaquesRequisicaoIT` PASS.
- [ ] **Step 5: Commit** — `feat(ataques): #1-#5 adulteração de requisição que altera o banco`.

---

### Task 3: Ataques de resposta e de fluxo (#6, #7, #10, #11, #12)

**Files:**
- Create: `.../ataques/RespostaFalsa.java`, `SaldoExibidoFalso.java`, `ValidacaoFalsificada.java`, `OperacaoSemValidacao.java`, `PassTheHash.java`
- Test: `.../ataques/AtaquesRespostaFluxoIT.java`

**Interfaces:**
- Consumes: Task 1; `Requisicoes` (incl. `loginComSenhaBruta`, `validarSenha`, `validarSenhaBruta`, `saque`, `saldo`), `SenhaHash` (protocolo), `BancoTeste`, `Rotulo`.
- Produces: cada classe implementa `Ataque`.

- [ ] **Step 1: Escrever os testes que falham**

```java
@Test void respostaFalsaEnganaOCliente() throws Exception {
    // saque de 100.00 em 100010 (saldo 0.00): servidor responde 003; regra troca p/ 000
    ContextoAtaque ctx = new ContextoAtaque(Ambiente.V1, BancoTeste.recriar(Ambiente.V1));
    assertEquals(Veredito.VULNERAVEL, new RespostaFalsa().avaliar(ctx));
    assertEquals(new BigDecimal("0.00"), BancoTeste.saldo(ctx.ds(), "0001", "100010")); // banco intacto
}
@Test void saldoExibidoFalso() throws Exception { /* consulta saldo de 100001; cliente recebe 9999.99 */ }
@Test void validacaoFalsificada() throws Exception {
    // validar_senha com senha errada -> 002; regra troca p/ 000; cliente recebe 000
}
@Test void operacaoSemValidacao() throws Exception {
    // envia saque direto (sem validar_senha antes); servidor efetiva -> VULNERAVEL
}
@Test void passTheHash() throws Exception {
    // hash = SenhaHash.md5("senha123"); novo login com loginComSenhaBruta("rodrigop", hash) -> 000
}
```

- [ ] **Step 2: Rodar e ver falhar** → FAIL.
- [ ] **Step 3: Implementar os cinco ataques.** `RespostaFalsa`/`ValidacaoFalsificada` carregam a regra de resposta e checam o `00090` recebido pelo cliente + banco; `SaldoExibidoFalso` checa o `00092` recebido; `OperacaoSemValidacao` envia só o saque e checa o banco; `PassTheHash` usa `SenhaHash.md5("senha123")` e `Requisicoes.loginComSenhaBruta`.
- [ ] **Step 4: Rodar e ver passar** — `mvn -B clean verify -Pintegracao -Dit.test=AtaquesRespostaFluxoIT` PASS.
- [ ] **Step 5: Commit** — `feat(ataques): #6,#7,#10,#11,#12 resposta falsa e falhas de fluxo`.

---

### Task 4: Replay (#8) e captura passiva (#9)

**Files:**
- Create: `.../ataques/Replay8.java`, `.../ataques/CapturaPassiva.java`
- Test: `.../ataques/AtaquesReplayCapturaIT.java`

**Interfaces:**
- Consumes: Task 1; `Replay` (interceptador, `reenviar(byte[], String, int, int)`), `CodecMensagem`, `Requisicoes`, `SenhaHash`, `BancoTeste`.
- Produces: `Replay8`, `CapturaPassiva` implementam `Ataque`.

- [ ] **Step 1: Escrever os testes que falham**

```java
@Test void replayDebitaVariasVezes() throws Exception {
    // saque de 100.00 em 100001 (1000.00) uma vez pelo proxy; depois Replay.reenviar 3x direto ao servidor
    // saldo final = 1000 - 100 - 3*100 = 600.00
    ContextoAtaque ctx = new ContextoAtaque(Ambiente.V1, BancoTeste.recriar(Ambiente.V1));
    assertEquals(Veredito.VULNERAVEL, new Replay8().avaliar(ctx));
    assertEquals(new BigDecimal("600.00"), BancoTeste.saldo(ctx.ds(), "0001", "100001"));
}
@Test void capturaPassivaVeOHashNoLog() throws Exception {
    // em passagem, faz login; logCapturado() contém SenhaHash.md5("senha123")
    ContextoAtaque ctx = new ContextoAtaque(Ambiente.V1, BancoTeste.recriar(Ambiente.V1));
    assertEquals(Veredito.VULNERAVEL, new CapturaPassiva().avaliar(ctx));
}
```

- [ ] **Step 2: Rodar e ver falhar** → FAIL.
- [ ] **Step 3: Implementar.** `Replay8.avaliar`: abre sessão em passagem, faz login + um saque, depois `Replay.reenviar(CodecMensagem.codificar(saque), "127.0.0.1", s.portaServidor(), 3)`; `VULNERAVEL` se o saldo caiu os reenvios. `CapturaPassiva.avaliar`: abre sessão em passagem, faz login, e `VULNERAVEL` se alguma linha de `s.logCapturado()` contém `SenhaHash.md5("senha123")`.
- [ ] **Step 4: Rodar e ver passar** — `mvn -B clean verify -Pintegracao -Dit.test=AtaquesReplayCapturaIT` PASS.
- [ ] **Step 5: Commit** — `feat(ataques): #8 replay e #9 captura passiva`.

---

### Task 5: Ataques offline (#13 quebra de hash, #14 chave AES)

**Files:**
- Create: `.../ataques/QuebraHash.java`, `.../ataques/ChaveAesNoCodigo.java`
- Test: `.../ataques/AtaquesOfflineTest.java` (sem banco, `mvn test`)

**Interfaces:**
- Consumes: `Ataque`, `Veredito`, `ContextoAtaque` (Task 1 — ignorado no veredito, mas mantém a assinatura); `SenhaHash` (protocolo); `CifraArquivo` (cliente).
- Produces: `QuebraHash`, `ChaveAesNoCodigo` implementam `Ataque`; `avaliar` não usa a rede.

- [ ] **Step 1: Escrever os testes que falham**

```java
// AtaquesOfflineTest
@Test void quebraHashRecuperaSenha() throws Exception {
    assertEquals(Veredito.VULNERAVEL, new QuebraHash().avaliar(null));
}
@Test void chaveAesDecifraOArquivo() throws Exception {
    assertEquals(Veredito.VULNERAVEL, new ChaveAesNoCodigo().avaliar(null));
}
```

- [ ] **Step 2: Rodar e ver falhar** → FAIL.
- [ ] **Step 3: Implementar.** `QuebraHash`: alvo `SenhaHash.md5("123456")`; percorre `000000`–`999999` calculando `SenhaHash.md5` até casar; `VULNERAVEL` se achou `"123456"`. `ChaveAesNoCodigo`: `CifraArquivo.decifrar(CifraArquivo.cifrar("rodrigop")).equals("rodrigop")` → `VULNERAVEL` (a chave está no código). Ambos ignoram o `contexto`.
- [ ] **Step 4: Rodar e ver passar** — `mvn -B -pl ccbank-tests -am test -Dtest=AtaquesOfflineTest -Dsurefire.failIfNoSpecifiedTests=false` PASS.
- [ ] **Step 5: Commit** — `feat(ataques): #13 quebra de hash e #14 chave AES no código`.

---

### Task 6: Registro, expectativas e `AtaquesIT`

**Files:**
- Create: `.../ataques/CatalogoAtaques.java`, `ccbank-tests/src/test/resources/expectativas-ataques.properties`
- Create: `.../ataques/AtaquesIT.java`
- Test: `.../ataques/CatalogoAtaquesTest.java`

**Interfaces:**
- Consumes: todos os `Ataque` (Tasks 2–5); `Veredito`, `ContextoAtaque`; `BancoTeste`, `Ambiente`.
- Produces:
  - `final class CatalogoAtaques` — `static List<Ataque> todos()` (os 14, na ordem do catálogo).
  - `final class ResultadosAtaques` — coletor estático `static void registrar(String id, String lado, Veredito v)` e `static Map<String,Map<String,Veredito>> resultados()` (usado pela Task 7).
  - `final class Expectativas` — `static Veredito ler(String id, String lado)` lê `expectativas-ataques.properties` do classpath; lança `IllegalStateException` com mensagem clara se a chave `<id>.<lado>` faltar (Review Focus 5).
  - `expectativas-ataques.properties` — 28 chaves `<id>.v1=VULNERAVEL` / `<id>.v2=VULNERAVEL`.
  - `AtaquesIT` — `@ParameterizedTest` sobre o produto (ataque × `Ambiente`); lê a expectativa, roda `avaliar`, registra o resultado e **falha se divergir**.

- [ ] **Step 1: Escrever `expectativas-ataques.properties`, `CatalogoAtaquesTest` e `AtaquesIT` (falhando)**

```java
// CatalogoAtaquesTest
@Test void catalogoTem14AtaquesComIdsUnicos() {
    List<Ataque> todos = CatalogoAtaques.todos();
    assertEquals(14, todos.size());
    assertEquals(14, todos.stream().map(Ataque::id).distinct().count());
}
// AtaquesIT
@ParameterizedTest @MethodSource("casos")
void ataqueBateComExpectativa(Ataque ataque, Ambiente amb) throws Exception {
    Veredito esperado = Expectativas.ler(ataque.id(), amb.lado().toLowerCase()); // falha claro se ausente
    Veredito real = ataque.avaliar(new ContextoAtaque(amb, BancoTeste.recriar(amb)));
    ResultadosAtaques.registrar(ataque.id(), amb.lado().toLowerCase(), real);
    assertEquals(esperado, real, ataque.id() + "/" + amb.lado());
}
```

- [ ] **Step 2: Rodar e ver falhar** → FAIL.
- [ ] **Step 3: Implementar** `CatalogoAtaques.todos()`, o coletor `ResultadosAtaques`, `Expectativas.ler` (erro claro se faltar a chave — cobre o Review Focus 5) e o `AtaquesIT` (fonte `casos()` = produto de `CatalogoAtaques.todos()` × `Ambiente.values()`).
- [ ] **Step 4: Rodar e ver passar** — `mvn -B clean verify -Pintegracao -Dit.test=AtaquesIT,CatalogoAtaquesTest` PASS (14 × 2 vereditos `VULNERAVEL`).
- [ ] **Step 5: Commit** — `feat(ataques): registro, expectativas e AtaquesIT (14 x 2 lados)`.

---

### Task 7: `GeradorMatrizAtaques` + emissão da matriz

**Files:**
- Create: `.../ataques/GeradorMatrizAtaques.java`
- Modify: `.../ataques/AtaquesIT.java` (emitir a matriz em `@AfterAll`)
- Test: `.../ataques/GeradorMatrizAtaquesTest.java`

**Interfaces:**
- Consumes: `Veredito`, `Ataque`, `ResultadosAtaques` (Tasks 1, 6).
- Produces: `final class GeradorMatrizAtaques` — `static String gerar(List<Ataque> ataques, Map<String,Map<String,Veredito>> resultados, java.time.LocalDateTime geradoEm)` (HTML autocontido) e `static java.nio.file.Path escrever(String html) throws IOException` (grava em `resultados/matriz-ataques-<AAAA-MM-DD-HHMMSS>.html`).

- [ ] **Step 1: Escrever o teste que falha**

```java
// GeradorMatrizAtaquesTest (mvn test)
@Test void matrizTemCelulasEVereditos() {
    Map<String,Map<String,Veredito>> r = new java.util.HashMap<>();
    r.put("valor-adulterado", mapOf("v1", Veredito.VULNERAVEL, "v2", Veredito.PROTEGIDO));
    String html = GeradorMatrizAtaques.gerar(CatalogoAtaques.todos(), r, java.time.LocalDateTime.now());
    assertTrue(html.contains("valor-adulterado"));
    assertTrue(html.contains("VULNERÁVEL"));
    assertTrue(html.contains("PROTEGIDO"));
    assertTrue(html.startsWith("<!DOCTYPE html>"));
}
```

- [ ] **Step 2: Rodar e ver falhar** → FAIL.
- [ ] **Step 3: Implementar `GeradorMatrizAtaques`** no estilo de `GeradorRelatorioHtml` (CSS embutido, `escapar`), tabela ataque × {v1, v2}, célula `VULNERÁVEL` (vermelha) / `PROTEGIDO` (verde); e ligar `AtaquesIT.@AfterAll` para gerar com `ResultadosAtaques.resultados()` e `escrever`.
- [ ] **Step 4: Rodar e ver passar** — `mvn -B -pl ccbank-tests -am test -Dtest=GeradorMatrizAtaquesTest -Dsurefire.failIfNoSpecifiedTests=false` PASS; depois `mvn -B clean verify -Pintegracao` verde no reator inteiro e a matriz aparece em `resultados/`.
- [ ] **Step 5: Commit** — `feat(ataques): matriz de ataques em HTML`.

---

## Verificação final

- `mvn -B clean verify -Pintegracao` verde no reator inteiro, rodando os 14 ataques × 2 lados.
- `regras/ataques-exemplo.json` carregável na tela e pelo harness.
- A matriz HTML é gerada em `resultados/` com todos `VULNERAVEL` nos dois lados.
- `expectativas-ataques.properties` com as 28 chaves; divergência faz o `AtaquesIT` falhar.
