# Fase 2b — Tela Swing do interceptador — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Pôr a tela Swing do interceptador sobre o núcleo da 2a: duas abas, três modos (passagem/manual/regras), histórico e replay, com um mediador plugável no proxy que preserva o headless.

**Architecture:** O proxy passa a consultar um `Mediador` por mensagem decodificável (em vez de aplicar regras fixo) e a notificar um `OuvinteTroca`. Mediadores de passagem, regras e manual; o modo manual bloqueia a thread do par até a tela resolver. A tela (JPanels testáveis + lógica fora do Swing) hospeda um `ServidorProxy` por aba. O headless da 2a é mantido por um construtor de conveniência.

**Tech Stack:** Java 8 (JDK 11, `--release 8`), Swing, `ccbank-protocol`, `ccbank-interceptor` (2a), JUnit 5.11.3.

**Spec:** `docs/superpowers/specs/2026-10-31-fase2b-tela-interceptador-design.md` (fonte de verdade); spec-mãe seção 7.

## Global Constraints

- `maven.compiler.release` = `8`; nenhuma API posterior ao Java 8. `sourceEncoding` = `UTF-8`. Pacote base `br.com.ccbank.interceptador`. Sem dependências novas.
- Limite de **65536 bytes** por mensagem (`CodecMensagem.TAMANHO_MAXIMO`). Timeout de leitura do cliente: 120 s (já existente), o que dá tempo ao modo manual.
- Portas: esquerdo 8001→9001, direito 8002→9002.
- **O headless da 2a não muda de comportamento:** o construtor `ServidorProxy(int, String, int, MotorRegras, String)` e `doLado(lado, MotorRegras)` permanecem; os testes da 2a seguem verdes.
- Replay é disparo **cru e direto ao servidor** (não passa pelo mediador).
- Lógica fora do Swing e testável; `java.awt.headless=true` no surefire do módulo. `JFrame` lança `HeadlessException` sob headless, então a janela e o `main` não são testados em unidade; `PainelAba` (JPanel) **não** abre a porta no construtor (um método `iniciar()` o faz).

## Review Focus

1. **Descartar requisição**: o servidor não recebe nada e o par **não** trava esperando uma resposta inexistente (pula o round-trip; o cliente acaba expirando). → teste na Task 2.
2. **Mensagem opaca em modo manual/regras**: passa opaca (bytes intactos), não vai para a tabela editável nem bloqueia. → teste na Task 2.
3. **Encaminhar original após editar a tabela**: manda os **bytes originais**, ignorando a edição. → teste na Task 3.
4. **Trocar de modo com uma mensagem retida**: a retida continua resolvível (não trava o próximo nem some). → teste na Task 4.
5. **Replay com `vezes <= 0` ou servidor que fecha cedo**: `vezes <= 0` não envia; fechamento precoce vira erro reportado, não trava. → teste na Task 6.

---

### Task 1: Núcleo de mediação — `Mediador`, `DecisaoMediacao`, `OuvinteTroca`, mediadores de passagem e regras

**Files:**
- Create: `.../mediacao/Mediador.java`, `DecisaoMediacao.java`, `OuvinteTroca.java`, `ModoInterceptador.java`, `MediadorPassagem.java`, `MediadorRegras.java`
- Test: `.../mediacao/MediadorPassagemTest.java`, `MediadorRegrasTest.java`

**Interfaces:**
- Consumes: `Mensagem` (protocolo); `MotorRegras`, `ResultadoAplicacao`, `Sentido` (2a).
- Produces:
  - `interface Mediador { DecisaoMediacao intervir(Mensagem mensagem, Sentido sentido); }`
  - `final class DecisaoMediacao` com `enum Acao { ENCAMINHAR, DESCARTAR }`; estáticos `encaminharOriginal(Mensagem m)` (ENCAMINHAR, `resultado = new ResultadoAplicacao(m, emptyList())`, `reserializar=false`), `encaminharModificada(ResultadoAplicacao r)` (ENCAMINHAR, `reserializar=true`), `descartar()` (DESCARTAR, `resultado=null`, `reserializar=false`); getters `Acao acao()`, `ResultadoAplicacao resultado()`, `boolean reserializar()`.
  - `interface OuvinteTroca { void observar(Sentido sentido, Mensagem original, DecisaoMediacao decisao); OuvinteTroca NULO = (s, o, d) -> {}; }`
  - `enum ModoInterceptador { PASSAGEM, MANUAL, REGRAS }`
  - `final class MediadorPassagem implements Mediador` — `intervir` devolve `encaminharOriginal(mensagem)`.
  - `final class MediadorRegras implements Mediador` — `MediadorRegras(MotorRegras motor)`; `intervir` faz `r = motor.aplicar(m, s)` e devolve `r.houveAdulteracao() ? encaminharModificada(r) : encaminharOriginal(m)`.

- [ ] **Step 1: Escrever os testes que falham**

```java
// MediadorPassagemTest
@Test void encaminhaOriginalSemReserializar() {
    DecisaoMediacao d = new MediadorPassagem().intervir(new Mensagem().adicionar("00001","saque"), Sentido.REQUISICAO);
    assertEquals(DecisaoMediacao.Acao.ENCAMINHAR, d.acao());
    assertFalse(d.reserializar());
}
// MediadorRegrasTest
@Test void comRegraQueCasaReserializa() {
    MotorRegras motor = new MotorRegras(singletonList(new Regra("x", true, Sentido.REQUISICAO,
        new Condicao(singletonMap("00001","saque")), singletonList(new Acao(Acao.Tipo.SET,"00030","0.01")))));
    DecisaoMediacao d = new MediadorRegras(motor).intervir(
        new Mensagem().adicionar("00001","saque").adicionar("00030","500.00"), Sentido.REQUISICAO);
    assertTrue(d.reserializar());
    assertEquals(Optional.of("0.01"), d.resultado().mensagem().primeiro("00030"));
}
@Test void semRegraQueCasaNaoReserializa() { /* 00001=deposito → encaminharOriginal, reserializar=false */ }
```

- [ ] **Step 2: Rodar e ver falhar** — `mvn -B -pl ccbank-interceptor -am test` → FAIL.
- [ ] **Step 3: Implementar** as classes do bloco Interfaces.
- [ ] **Step 4: Rodar e ver passar** → PASS.
- [ ] **Step 5: Commit** — `feat(interceptador): núcleo de mediação (Mediador, DecisaoMediacao, passagem e regras)`.

---

### Task 2: Refactor do proxy para o mediador (descarte, opaco, ouvinte)

**Files:**
- Modify: `.../proxy/ParDeConexoes.java`, `.../proxy/ServidorProxy.java`
- Test: `.../proxy/ServidorProxyTest.java` (acrescentar casos)

**Interfaces:**
- Consumes: `Mediador`, `DecisaoMediacao`, `OuvinteTroca` (Task 1); `MotorRegras` (2a).
- Produces:
  - `ParDeConexoes(Socket cliente, String hostDestino, int portaDestino, Mediador mediador, OuvinteTroca ouvinte, String sufixoLog, Set<Socket> registro)`.
  - `ServidorProxy` ganha o construtor `ServidorProxy(int portaEscuta, String hostDestino, int portaDestino, Mediador mediador, OuvinteTroca ouvinte, String sufixoLog)` e `static ServidorProxy doLado(LadoInterceptador lado, Mediador mediador, OuvinteTroca ouvinte)`. **Mantém** `ServidorProxy(int, String, int, MotorRegras, String)` e `doLado(lado, MotorRegras)` delegando a `new MediadorRegras(motor)` com `OuvinteTroca.NULO`.

- [ ] **Step 1: Escrever os testes que falham** (reusam o servidor-eco da 2a; `MediadorManual` ainda não existe, então o descarte é exercido com um `Mediador` de teste anônimo que devolve `descartar()`)

```java
@Test void descartarRequisicaoNaoChegaAoServidor() throws Exception {
    Mediador descarta = (m, s) -> DecisaoMediacao.descartar();
    // proxy com esse mediador; cliente envia uma requisição; servidor-eco NUNCA recebe um quadro
    // (usar ServidorFalso com resposta null e asserta eco.recebido == null após um tempo curto)
}
@Test void descartarRespostaNaoChegaAoCliente() throws Exception {
    // mediador que descarta só RESPOSTA; eco responde; cliente não recebe quadro (read time-out/charge)
}
@Test void ouvinteRecebeCadaTroca() throws Exception {
    // ouvinte coletor; passagem; asserta que observou REQUISICAO (e RESPOSTA) com a transação certa
}
@Test void opacoNaoVaiAoMediador() throws Exception {
    // mediador que lançaria se chamado; cliente envia "sem-igual\n\n"; servidor-eco recebe os bytes crus
}
```

- [ ] **Step 2: Rodar e ver falhar** → FAIL (construtor novo inexistente).
- [ ] **Step 3: Refatorar `ParDeConexoes` e `ServidorProxy`.** `repassar` passa a: decodificar → se `MensagemMalformadaException`, modo opaco (escreve bytes originais, **não** chama o mediador nem o ouvinte) e devolve `true`; senão `decisao = mediador.intervir(msg, sentido)`, `ouvinte.observar(sentido, msg, decisao)`, e se `DESCARTAR` devolve `false` (nada escrito), senão escreve `decisao.reserializar() ? codificar(decisao.resultado().mensagem()) : quadroOriginal` e devolve `true`. O loop de `executar`: após a requisição, se `repassar(...)` devolveu `false` (descartada), `continue` (pula a leitura da resposta); a resposta descartada apenas não é escrita. Logar as etapas como hoje.
- [ ] **Step 4: Rodar e ver passar** — `mvn -B -pl ccbank-interceptor -am test` → PASS (inclui os testes da 2a inalterados).
- [ ] **Step 5: Commit** — `refactor(interceptador): proxy consulta mediador e ouvinte; trata descarte`.

---

### Task 3: `ControladorManual` (retenção bloqueante + filtro)

**Files:**
- Create: `.../mediacao/ControladorManual.java`
- Test: `.../mediacao/ControladorManualTest.java`

**Interfaces:**
- Consumes: `Mediador`, `DecisaoMediacao` (Task 1); `Mensagem`, `Sentido`.
- Produces:
  - `final class ControladorManual implements Mediador` com `enum Filtro { REQUISICOES, RESPOSTAS, AMBAS }`.
  - `ControladorManual(Filtro filtro)`; `void filtro(Filtro f)`; `Filtro filtro()`.
  - `DecisaoMediacao intervir(Mensagem m, Sentido s)` — se o sentido **não** é retido pelo filtro, devolve `encaminharOriginal(m)` sem bloquear; se é, publica a retenção (`retida`/`sentidoRetido`), dispara o ouvinte de retenção e **bloqueia** até a tela resolver; se a thread for interrompida, devolve `descartar()`.
  - `Optional<Mensagem> retida()`; `Sentido sentidoRetido()`; `void aoMudarRetencao(Runnable aoMudar)` (chamado quando retém e quando libera).
  - `void encaminhar(Mensagem editada)` → resolve com `encaminharModificada(new ResultadoAplicacao(editada, emptyList()))`; `void encaminharOriginal()` → resolve com `encaminharOriginal(<retida>)`; `void descartar()` → resolve com `descartar()`.

- [ ] **Step 1: Escrever os testes que falham** (com threads; a retenção é resolvida de outra thread)

```java
@Test void sentidoForaDoFiltroNaoBloqueia() {
    ControladorManual c = new ControladorManual(ControladorManual.Filtro.RESPOSTAS);
    DecisaoMediacao d = c.intervir(new Mensagem().adicionar("00001","saque"), Sentido.REQUISICAO);
    assertEquals(DecisaoMediacao.Acao.ENCAMINHAR, d.acao());
    assertFalse(d.reserializar());
}
@Test void retemEEncaminhaEditada() throws Exception {
    ControladorManual c = new ControladorManual(ControladorManual.Filtro.AMBAS);
    CompletableFuture<DecisaoMediacao> f = CompletableFuture.supplyAsync(
        () -> c.intervir(new Mensagem().adicionar("00030","500.00"), Sentido.REQUISICAO));
    aguardarAte(() -> c.retida().isPresent(), 2000); // helper de espera
    c.encaminhar(new Mensagem().adicionar("00030","0.01"));
    DecisaoMediacao d = f.get(2, TimeUnit.SECONDS);
    assertTrue(d.reserializar());
    assertEquals(Optional.of("0.01"), d.resultado().mensagem().primeiro("00030"));
    assertFalse(c.retida().isPresent());
}
@Test void encaminharOriginalIgnoraEdicao() throws Exception {
    // retém 00030=500.00; chama encaminharOriginal(); decisão reserializar=false, resultado.mensagem()==original
}
@Test void descartarDevolveDescarte() throws Exception { /* decisão DESCARTAR */ }
```

- [ ] **Step 2: Rodar e ver falhar** → FAIL.
- [ ] **Step 3: Implementar `ControladorManual`** com uma entrega sincronizada (`SynchronousQueue<DecisaoMediacao>` ou lock+condição): `intervir` guarda a retida, dispara `aoMudarRetencao`, faz `take()`; as três ações de resolução fazem `put(...)` e limpam a retida. Guardar a mensagem original para `encaminharOriginal()`.
- [ ] **Step 4: Rodar e ver passar** → PASS.
- [ ] **Step 5: Commit** — `feat(interceptador): modo manual com retenção bloqueante e filtro`.

---

### Task 4: `MediadorModo` (delegação por modo, troca em runtime)

**Files:**
- Create: `.../mediacao/MediadorModo.java`
- Test: `.../mediacao/MediadorModoTest.java`

**Interfaces:**
- Consumes: `Mediador`, `MediadorPassagem`, `MediadorRegras`, `ControladorManual`, `ModoInterceptador` (Tasks 1, 3); `MotorRegras`, `Regra` (2a).
- Produces:
  - `final class MediadorModo implements Mediador` — `MediadorModo(ControladorManual manual)`; começa em `PASSAGEM`.
  - `void modo(ModoInterceptador m)`; `ModoInterceptador modo()`; `void carregarRegras(List<Regra> regras)` (recria o `MediadorRegras` interno); `ControladorManual controladorManual()`.
  - `DecisaoMediacao intervir(Mensagem m, Sentido s)` delega ao mediador do modo atual.

- [ ] **Step 1: Escrever os testes que falham**

```java
@Test void delegaConformeOModo() {
    MediadorModo mm = new MediadorModo(new ControladorManual(ControladorManual.Filtro.AMBAS));
    assertEquals(ModoInterceptador.PASSAGEM, mm.modo());
    // PASSAGEM: encaminha sem reserializar
    assertFalse(mm.intervir(new Mensagem().adicionar("00001","saque"), Sentido.REQUISICAO).reserializar());
    // REGRAS após carregar uma regra que casa: reserializa
    mm.carregarRegras(singletonList(new Regra("x", true, Sentido.REQUISICAO,
        new Condicao(singletonMap("00001","saque")), singletonList(new Acao(Acao.Tipo.SET,"00030","0.01")))));
    mm.modo(ModoInterceptador.REGRAS);
    assertTrue(mm.intervir(new Mensagem().adicionar("00001","saque").adicionar("00030","1.00"), Sentido.REQUISICAO).reserializar());
}
@Test void retidaContinuaResolvivelAposTrocarModo() throws Exception {
    // modo MANUAL, retém uma requisição numa thread; troca mm.modo(PASSAGEM);
    // controladorManual().encaminhar(...) ainda resolve a retida (não trava)
}
```

- [ ] **Step 2: Rodar e ver falhar** → FAIL.
- [ ] **Step 3: Implementar `MediadorModo`** guardando os três mediadores; `intervir` escolhe por `modo` (volátil).
- [ ] **Step 4: Rodar e ver passar** → PASS.
- [ ] **Step 5: Commit** — `feat(interceptador): mediador por modo com troca em runtime`.

---

### Task 5: `Historico` + `TrocaRegistrada` (OuvinteTroca)

**Files:**
- Create: `.../historico/TrocaRegistrada.java`, `.../historico/Historico.java`
- Test: `.../historico/HistoricoTest.java`

**Interfaces:**
- Consumes: `OuvinteTroca`, `DecisaoMediacao` (Tasks 1); `Mensagem`, `Rotulo`, `Sentido`.
- Produces:
  - `final class TrocaRegistrada` — campos `java.time.LocalDateTime hora`, `Sentido sentido`, `String transacao`, `boolean adulterada`, `DecisaoMediacao.Acao acao`, `Mensagem original`, `Mensagem resultante`; getters.
  - `final class Historico implements OuvinteTroca` — `void observar(Sentido, Mensagem, DecisaoMediacao)` acrescenta uma `TrocaRegistrada`; `List<TrocaRegistrada> trocas()`; `void limpar()`. `transacao` = valor de `Rotulo.TRANSACAO` (`00001`) do original, ou vazio. `adulterada` = `decisao.reserializar()`. `resultante` = `decisao.resultado()!=null ? decisao.resultado().mensagem() : original`.

- [ ] **Step 1: Escrever os testes que falham**

```java
@Test void registraComTransacaoEAdulteracao() {
    Historico h = new Historico();
    Mensagem orig = new Mensagem().adicionar("00001","saque").adicionar("00030","500.00");
    h.observar(Sentido.REQUISICAO, orig, DecisaoMediacao.encaminharModificada(
        new ResultadoAplicacao(new Mensagem().adicionar("00001","saque").adicionar("00030","0.01"), emptyList())));
    assertEquals(1, h.trocas().size());
    assertEquals("saque", h.trocas().get(0).transacao());
    assertTrue(h.trocas().get(0).adulterada());
}
@Test void passagemNaoEhAdulterada() { /* encaminharOriginal → adulterada=false */ }
@Test void descarteRegistraAcaoDescartar() { /* descartar → acao==DESCARTAR, resultante==original */ }
```

- [ ] **Step 2: Rodar e ver falhar** → FAIL.
- [ ] **Step 3: Implementar** `TrocaRegistrada` e `Historico`.
- [ ] **Step 4: Rodar e ver passar** → PASS.
- [ ] **Step 5: Commit** — `feat(interceptador): histórico de trocas por aba`.

---

### Task 6: `Replay` (reenvio direto ao servidor)

**Files:**
- Create: `.../proxy/Replay.java`
- Test: `.../proxy/ReplayTest.java` (servidor-eco em laço, porta efêmera)

**Interfaces:**
- Consumes: `CodecMensagem` (`lerQuadro`, `TAMANHO_MAXIMO`); nada do Swing.
- Produces:
  - `final class Replay` — `static List<byte[]> reenviar(byte[] requisicao, String host, int porta, int vezes) throws IOException`. `vezes <= 0` devolve lista vazia sem conectar. Abre **uma** conexão ao `host:porta`, escreve os bytes `vezes` vezes, lendo um quadro de resposta a cada vez (`CodecMensagem.lerQuadro`), e devolve as respostas (cada uma como `byte[]`). Se o servidor fechar antes de uma resposta, encerra com `IOException`.

- [ ] **Step 1: Escrever os testes que falham** (servidor-eco de teste que lê um quadro e devolve uma resposta, em laço até o cliente fechar)

```java
@Test void reenviaNVezesERecebeNRespostas() throws Exception {
    byte[] req = CodecMensagem.codificar(new Mensagem().adicionar("00001","saque"));
    try (EcoEmLaco eco = new EcoEmLaco(CodecMensagem.codificar(new Mensagem().adicionar("00090","000")))) {
        List<byte[]> respostas = Replay.reenviar(req, "127.0.0.1", eco.porta(), 3);
        assertEquals(3, respostas.size());
        assertEquals(3, eco.quadrosRecebidos());
    }
}
@Test void vezesZeroNaoConecta() throws Exception {
    assertTrue(Replay.reenviar(new byte[]{1}, "127.0.0.1", 1, 0).isEmpty());
}
@Test void servidorQueFechaCedoLancaIOException() throws Exception {
    // eco que fecha após a 1a leitura; reenviar(..., 3) lança IOException
}
```

- [ ] **Step 2: Rodar e ver falhar** → FAIL.
- [ ] **Step 3: Implementar `Replay`.**
- [ ] **Step 4: Rodar e ver passar** → PASS.
- [ ] **Step 5: Commit** — `feat(interceptador): replay de requisição direto ao servidor`.

---

### Task 7: Modelos de tabela Swing — `ModeloCamposEditaveis`, `ModeloHistorico`

**Files:**
- Create: `.../tela/ModeloCamposEditaveis.java`, `.../tela/ModeloHistorico.java`
- Test: `.../tela/ModeloCamposEditaveisTest.java`, `.../tela/ModeloHistoricoTest.java`

**Interfaces:**
- Consumes: `Mensagem`, `Campo`, `Rotulo`; `Historico`, `TrocaRegistrada`.
- Produces:
  - `final class ModeloCamposEditaveis extends javax.swing.table.AbstractTableModel` — colunas rótulo | nome (derivado de `Rotulo.porCodigo`, não editável) | valor (editável). `void popular(Mensagem m)`; `Mensagem montarMensagem()`; `void adicionarLinha()`; `void removerLinha(int i)`.
  - `final class ModeloHistorico extends javax.swing.table.AbstractTableModel` — colunas hora | sentido | transação | adulterada; `ModeloHistorico(Historico h)`; `void atualizar()` (relê `h.trocas()`); `TrocaRegistrada linha(int i)`.

- [ ] **Step 1: Escrever os testes que falham** (rodam headless)

```java
// ModeloCamposEditaveisTest
@Test void montaMensagemDasLinhas() {
    ModeloCamposEditaveis m = new ModeloCamposEditaveis();
    m.popular(new Mensagem().adicionar("00001","saque").adicionar("00030","500.00"));
    assertEquals(2, m.getRowCount());
    m.setValueAt("0.01", 1, 2); // edita o valor da 2a linha
    assertEquals(Optional.of("0.01"), m.montarMensagem().primeiro("00030"));
}
@Test void adicionaERemoveLinha() { /* adicionarLinha incrementa; removerLinha decrementa */ }
// ModeloHistoricoTest
@Test void refleteOHistorico() {
    Historico h = new Historico();
    h.observar(Sentido.REQUISICAO, new Mensagem().adicionar("00001","saque"), DecisaoMediacao.encaminharOriginal(new Mensagem().adicionar("00001","saque")));
    ModeloHistorico mh = new ModeloHistorico(h);
    mh.atualizar();
    assertEquals(1, mh.getRowCount());
    assertEquals("saque", mh.linha(0).transacao());
}
```

- [ ] **Step 2: Rodar e ver falhar** → FAIL.
- [ ] **Step 3: Implementar os dois modelos.**
- [ ] **Step 4: Rodar e ver passar** → PASS.
- [ ] **Step 5: Commit** — `feat(interceptador): modelos de tabela de campos e de histórico`.

---

### Task 8: Shell Swing — `PainelAba`, `InterceptadorTelaMain`, script

**Files:**
- Create: `.../tela/PainelAba.java`, `.../tela/InterceptadorTelaMain.java`
- Create: `scripts/start-interceptador.ps1`
- Modify: `ccbank-interceptor/pom.xml` (surefire `java.awt.headless=true`; shade de uma 2a jar separada para a tela — ver passo 3)
- Test: `.../tela/PainelAbaTest.java`

**Interfaces:**
- Consumes: `LadoInterceptador`, `MediadorModo`, `ControladorManual`, `Historico`, `Replay`, `ServidorProxy`, `ModeloCamposEditaveis`, `ModeloHistorico`, `CarregadorRegras`.
- Produces:
  - `final class PainelAba extends javax.swing.JPanel` — `PainelAba(LadoInterceptador lado)`; **não** abre a porta no construtor; `void iniciar() throws IOException` cria `ServidorProxy.doLado(lado, mediadorModo, historico)` e chama `iniciar()`; `void parar()`; `ModoInterceptador modoAtual()` (começa em `PASSAGEM`). Monta o seletor de modo, carregar regras (erro de regras inválidas em `JOptionPane`, mantendo as anteriores), o painel manual (tabela editável + filtro + Encaminhar/Encaminhar original/Descartar) e a tabela de histórico com Reenviar (pede `vezes`, roda `Replay` em `SwingWorker`).
  - `final class InterceptadorTelaMain` — `public static void main(String[] args)` monta um `JFrame` 1280×800 com `JTabbedPane` de `PainelAba(ESQUERDO)` e `PainelAba(DIREITO)`, chama `iniciar()` de cada um na EDT e trata `IOException` (porta em uso) com aviso na aba.

- [ ] **Step 1: Escrever o teste que falha** (headless; só o JPanel, sem abrir porta)

```java
// PainelAbaTest
@Test void estadoInicialEhPassagemSemAbrirPorta() {
    PainelAba aba = new PainelAba(LadoInterceptador.ESQUERDO);
    assertEquals(ModoInterceptador.PASSAGEM, aba.modoAtual());
    // não chamamos iniciar(), então nada abre a 8001
}
```

- [ ] **Step 2: Rodar e ver falhar** → FAIL.
- [ ] **Step 3: Implementar `PainelAba` e `InterceptadorTelaMain`.** Acrescentar ao `pom.xml` o `systemPropertyVariables`/`java.awt.headless=true` no surefire e uma execução de shade com `finalName` próprio (ex.: `ccbank-interceptor-tela`, `mainClass` `...tela.InterceptadorTelaMain`) **além** da shade headless existente (`ccbank-interceptor` → `InterceptadorMain`). Criar `scripts/start-interceptador.ps1` no estilo de `start-cliente.ps1` (empacota se preciso; `javaw -jar ...ccbank-interceptor-tela.jar`).
- [ ] **Step 4: Rodar e ver passar** — `mvn -B -pl ccbank-interceptor -am test` → PASS; depois `mvn -B clean verify -Pintegracao` verde no reator inteiro.
- [ ] **Step 5: Commit** — `feat(interceptador): tela Swing com abas, modos, histórico e replay`.

---

## Verificação final

- `mvn -B clean verify -Pintegracao` verde no reator inteiro; headless da 2a inalterado.
- Mediadores, `ControladorManual`, `MediadorModo`, `Historico`, `Replay` e os modelos de tabela cobertos por teste.
- A tela abre com duas abas; cada aba troca de modo, carrega regras, retém/edita/encaminha/descarta no manual, mostra histórico e reenvia N vezes.
