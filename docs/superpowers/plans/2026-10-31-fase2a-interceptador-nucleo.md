# Fase 2a — Núcleo do interceptador (headless) — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Criar o módulo `ccbank-interceptor` com o motor de regras (função pura sobre `Mensagem`), o proxy TCP por lado e a CLI headless, provando adulteração de mensagens ponta a ponta sem tela.

**Architecture:** O motor (`regras/`) é puro e sem rede: casa condições, aplica `SET`/`ADD`/`REMOVE` e devolve a mensagem nova mais a lista de adulterações. O proxy (`proxy/`) é uma camada fina que lê um quadro com o codec do protocolo, chama o motor no sentido certo e repassa — reencaminhando os bytes originais quando nada mudou. `InterceptadorMain` sobe um lado por processo, como os servidores v1/v2.

**Tech Stack:** Java (bytecode 8, compilado com JDK 11), Maven 3.9, `ccbank-protocol`, Jackson 2.17.2, SLF4J + Logback 1.3.14, JUnit 5.11.3.

**Spec:** `docs/superpowers/specs/2026-10-31-fase2a-interceptador-nucleo-design.md` (fonte de verdade); spec-mãe `docs/superpowers/specs/2026-10-31-ccbank-laboratorio-design.md`, seções 3.3, 4, 6, 7, 9.

## Global Constraints

- `maven.compiler.release` = `8`; nenhuma API posterior ao Java 8. `sourceEncoding` = `UTF-8`.
- groupId `br.com.ccbank`, versão `0.1.0-SNAPSHOT`, pacote base `br.com.ccbank.interceptador`.
- Versões vêm do `dependencyManagement` do pai: `jackson-databind` 2.17.2, `slf4j-api` 2.0.16, `logback-classic` 1.3.14, `junit-jupiter` 5.11.3. Nenhuma dependência nova no pai.
- Limite de **65536 bytes** por mensagem (constante `CodecMensagem.TAMANHO_MAXIMO`), igual nos dois lados.
- Topologia (spec-mãe 3.3): esquerdo escuta 8001 → destino 127.0.0.1:9001; direito escuta 8002 → 127.0.0.1:9002.
- `Sentido` (REQUISICAO/RESPOSTA) é do interceptador e é distinto de `Direcao` (ENVIADA/RECEBIDA) do `ccbank-protocol`, usado só na formatação de log.
- `SET` em rótulo ausente é **no-op** (criar campo é só com `ADD`). `ADD` sempre acrescenta ao final. `REMOVE` tira **todas** as ocorrências.
- `ativa` ausente no JSON assume `true`. Regras aplicadas na **ordem** do arquivo; a condição de cada regra é avaliada contra o estado **corrente** da mensagem (após as regras anteriores).
- Condição: todos os rótulos de `quando` têm de casar contra a **primeira** ocorrência do rótulo; literal compara por igualdade; prefixo `~` é regex avaliada com `matches()` (casa o valor inteiro).
- O motor **não muta** a mensagem de entrada: devolve uma `Mensagem` nova.
- Em modo passagem e em qualquer troca sem adulteração, o proxy reencaminha os **bytes originais do quadro** (sem reserializar), preservando fidelidade (inclusive CRLF).
- CLI headless: `InterceptadorMain --headless --lado=esquerdo|direito [--regras=<arquivo.json>]`. Falha (porta, args, arquivo) → log + código de saída 1, como `ServidorMain`.
- Logs por lado em `logs/interceptor-<lado>.log`, rotação diária, retenção 30; as quatro etapas por troca com nomes legíveis; campo adulterado marcado com valor anterior → novo e a regra.

## Review Focus

1. **Rótulo repetido sob `SET`**: todas as ocorrências são alteradas e cada uma gera uma `Adulteracao`. → teste na Task 2.
2. **Regex inválida em `quando`** (ex.: `~(`): erro claro no carregamento, nunca `PatternSyntaxException` crua subindo do proxy. → testes nas Tasks 2 e 4.
3. **Quadro que não decodifica** (linha sem `=`) e **quadro > 64 KB**: o primeiro passa em modo opaco (bytes intactos); o segundo derruba o par. → testes na Task 6.
4. **`tipo` de ação ou `direcao` inválidos no JSON**: `RegrasInvalidasException` com mensagem clara, não `NullPointerException`. → teste na Task 4.
5. **Fidelidade de bytes em passagem** (mensagem com CRLF não adulterada): chega idêntica ao servidor, sem normalização. → teste na Task 6.

---

### Task 1: Módulo Maven + `Sentido` + `LadoInterceptador`

**Files:**
- Modify: `pom.xml` (acrescentar `<module>ccbank-interceptor</module>` após `ccbank-client`)
- Create: `ccbank-interceptor/pom.xml` (deps: `ccbank-protocol`, `jackson-databind`, `slf4j-api`, `logback-classic` runtime, `junit-jupiter` test)
- Create: `ccbank-interceptor/src/main/java/br/com/ccbank/interceptador/regras/Sentido.java`
- Create: `ccbank-interceptor/src/main/java/br/com/ccbank/interceptador/config/LadoInterceptador.java`
- Test: `ccbank-interceptor/src/test/java/br/com/ccbank/interceptador/config/LadoInterceptadorTest.java`

**Interfaces:**
- Produces:
  - `enum Sentido { REQUISICAO, RESPOSTA }` com `static Sentido porNome(String nome)` (case-insensitive; nome inválido lança `IllegalArgumentException`).
  - `enum LadoInterceptador { ESQUERDO, DIREITO }` com `int portaEscuta()` (8001/8002), `String hostDestino()` (`"127.0.0.1"`), `int portaDestino()` (9001/9002), `String sufixoLog()` (`"esquerdo"`/`"direito"`), `String sigla()` (`"ESQ"`/`"DIR"`), `static LadoInterceptador porNome(String nome)` (case-insensitive; inválido lança `IllegalArgumentException`).

- [ ] **Step 1: Escrever o `pom.xml` do módulo, registrar no pai e escrever o teste que falha**

```java
// LadoInterceptadorTest
@Test void portasDoEsquerdo() {
    assertEquals(8001, LadoInterceptador.ESQUERDO.portaEscuta());
    assertEquals(9001, LadoInterceptador.ESQUERDO.portaDestino());
    assertEquals("127.0.0.1", LadoInterceptador.ESQUERDO.hostDestino());
}
@Test void portasDoDireito() {
    assertEquals(8002, LadoInterceptador.DIREITO.portaEscuta());
    assertEquals(9002, LadoInterceptador.DIREITO.portaDestino());
}
@Test void porNomeIgnoraCaixa() { assertEquals(LadoInterceptador.ESQUERDO, LadoInterceptador.porNome("Esquerdo")); }
@Test void porNomeInvalidoLanca() { assertThrows(IllegalArgumentException.class, () -> LadoInterceptador.porNome("meio")); }
```

- [ ] **Step 2: Rodar e ver falhar** — `mvn -B -pl ccbank-interceptor test` → FAIL (classes inexistentes).
- [ ] **Step 3: Implementar `Sentido` e `LadoInterceptador`** com as assinaturas do bloco Interfaces.
- [ ] **Step 4: Rodar e ver passar** — `mvn -B -pl ccbank-interceptor test` → PASS. Rodar `mvn -B clean verify -Pintegracao` para confirmar que o reator aceita o módulo novo.
- [ ] **Step 5: Commit** — `feat(interceptador): módulo ccbank-interceptor, Sentido e LadoInterceptador`.

---

### Task 2: Casamento e mutação — `Condicao`, `Adulteracao`, `Acao`

**Files:**
- Create: `.../regras/Condicao.java`, `.../regras/Adulteracao.java`, `.../regras/Acao.java`
- Test: `.../regras/CondicaoTest.java`, `.../regras/AcaoTest.java`

**Interfaces:**
- Consumes: `br.com.ccbank.protocolo.Mensagem`, `Campo` (de `ccbank-protocol`: `Campo(String rotulo, String valor)`, `rotulo()`, `valor()`; `Mensagem.primeiro(String)→Optional<String>`, `Mensagem.campos()→List<Campo>`).
- Produces:
  - `final class Condicao`: `Condicao(Map<String,String> padroes)` (compila eagermente os padrões `~`; padrão regex inválido lança `IllegalArgumentException`); `boolean casa(Mensagem m)`; `Map<String,String> padroes()`.
  - `final class Adulteracao`: `Adulteracao(String rotulo, String antes, String depois, String regra)` (`antes==null` = campo adicionado; `depois==null` = campo removido); getters `rotulo/antes/depois/regra`; `equals`/`hashCode`/`toString`.
  - `final class Acao`: `enum Tipo { SET, ADD, REMOVE }`; `Acao(Tipo tipo, String rotulo, String valor)`; `List<Adulteracao> aplicar(List<Campo> campos, String nomeRegra)` (muta `campos`); getters `tipo/rotulo/valor`.

- [ ] **Step 1: Escrever os testes que falham**

```java
// CondicaoTest
@Test void literalCasa() {
    Mensagem m = new Mensagem().adicionar("00001","saque");
    assertTrue(new Condicao(singletonMap("00001","saque")).casa(m));
}
@Test void literalNaoCasa() { /* 00001=deposito, condição saque → false */ }
@Test void regexCasaValorInteiro() {
    Mensagem m = new Mensagem().adicionar("00030","1500.00");
    assertTrue(new Condicao(singletonMap("00030","~[0-9.]+")).casa(m));
}
@Test void regexNaoCasaParcial() {
    Mensagem m = new Mensagem().adicionar("00020","100001");
    assertFalse(new Condicao(singletonMap("00020","~1000")).casa(m)); // matches() exige o valor inteiro
}
@Test void todosOsRotulosPrecisamCasar() { /* dois pares, um não casa → false */ }
@Test void rotuloAusenteNaoCasa() { assertFalse(new Condicao(singletonMap("00001","saque")).casa(new Mensagem())); }
@Test void regexInvalidaLancaNaConstrucao() {
    assertThrows(IllegalArgumentException.class, () -> new Condicao(singletonMap("00030","~(")));
}

// AcaoTest  (helper: List<Campo> de uma Mensagem via new ArrayList<>(m.campos()))
@Test void setAlteraUmaOcorrencia() {
    List<Campo> campos = new ArrayList<>(new Mensagem().adicionar("00030","500.00").campos());
    List<Adulteracao> ad = new Acao(Acao.Tipo.SET,"00030","5.00").aplicar(campos, "r");
    assertEquals("5.00", campos.get(0).valor());
    assertEquals(new Adulteracao("00030","500.00","5.00","r"), ad.get(0));
}
@Test void setAlteraTodasAsOcorrencias() { /* dois campos 00060 repetidos → ambos mudam; 2 adulterações */ }
@Test void setEmRotuloAusenteEhNoOp() {
    List<Campo> campos = new ArrayList<>(new Mensagem().adicionar("00001","saque").campos());
    assertTrue(new Acao(Acao.Tipo.SET,"00030","5.00").aplicar(campos,"r").isEmpty());
    assertEquals(1, campos.size());
}
@Test void addAcrescentaAoFinal() {
    List<Campo> campos = new ArrayList<>(new Mensagem().adicionar("00001","saque").campos());
    List<Adulteracao> ad = new Acao(Acao.Tipo.ADD,"00030","9.99").aplicar(campos,"r");
    assertEquals("00030", campos.get(campos.size()-1).rotulo());
    assertEquals(new Adulteracao("00030", null, "9.99", "r"), ad.get(0));
}
@Test void removeTiraTodasAsOcorrencias() {
    List<Campo> campos = new ArrayList<>(new Mensagem().adicionar("00030","1").adicionar("00030","2").campos());
    List<Adulteracao> ad = new Acao(Acao.Tipo.REMOVE,"00030",null).aplicar(campos,"r");
    assertTrue(campos.isEmpty());
    assertEquals(2, ad.size());
    assertNull(ad.get(0).depois());
}
```

- [ ] **Step 2: Rodar e ver falhar** — `mvn -B -pl ccbank-interceptor test` → FAIL.
- [ ] **Step 3: Implementar `Condicao`, `Adulteracao`, `Acao`.** `Condicao` guarda, por rótulo, um literal ou um `Pattern` já compilado; `casa` usa `m.primeiro(rotulo)`. `Acao.aplicar` percorre `campos` substituindo `Campo` por `new Campo(rotulo, valor)` no `SET`, `campos.add` no `ADD`, e `removeIf` no `REMOVE`, registrando uma `Adulteracao` por efeito.
- [ ] **Step 4: Rodar e ver passar** — `mvn -B -pl ccbank-interceptor test` → PASS.
- [ ] **Step 5: Commit** — `feat(interceptador): condição (literal/regex), ação SET/ADD/REMOVE e adulteração`.

---

### Task 3: Composição — `Regra`, `ResultadoAplicacao`, `MotorRegras`

**Files:**
- Create: `.../regras/Regra.java`, `.../regras/ResultadoAplicacao.java`, `.../regras/MotorRegras.java`
- Test: `.../regras/MotorRegrasTest.java`

**Interfaces:**
- Consumes: `Sentido`, `Condicao`, `Acao`, `Adulteracao` (Tasks 1–2); `Mensagem`, `Campo`.
- Produces:
  - `final class Regra`: `Regra(String nome, boolean ativa, Sentido sentido, Condicao condicao, List<Acao> acoes)`; getters `nome/ativa/sentido/condicao/acoes`.
  - `final class ResultadoAplicacao`: `ResultadoAplicacao(Mensagem mensagem, List<Adulteracao> adulteracoes)`; `Mensagem mensagem()`; `List<Adulteracao> adulteracoes()`; `boolean houveAdulteracao()`.
  - `final class MotorRegras`: `MotorRegras(List<Regra> regras)`; `ResultadoAplicacao aplicar(Mensagem mensagem, Sentido sentido)`.

- [ ] **Step 1: Escrever os testes que falham**

```java
// MotorRegrasTest
private static Regra regra(String nome, boolean ativa, Sentido s, Condicao c, Acao... a) {
    return new Regra(nome, ativa, s, c, asList(a));
}
@Test void aplicaRegraDoSentidoQueCasa() {
    MotorRegras m = new MotorRegras(singletonList(regra("saque-centavos", true, Sentido.REQUISICAO,
        new Condicao(singletonMap("00001","saque")), new Acao(Acao.Tipo.SET,"00030","0.01"))));
    ResultadoAplicacao r = m.aplicar(new Mensagem().adicionar("00001","saque").adicionar("00030","500.00"), Sentido.REQUISICAO);
    assertEquals(Optional.of("0.01"), r.mensagem().primeiro("00030"));
    assertTrue(r.houveAdulteracao());
}
@Test void ignoraRegraInativa() { /* ativa=false → mensagem intacta, sem adulteração */ }
@Test void ignoraSentidoOposto() { /* regra RESPOSTA ao aplicar REQUISICAO → intacta */ }
@Test void ignoraCondicaoQueNaoCasa() { /* 00001=deposito, regra exige saque → intacta */ }
@Test void semRegrasAplicaveisDevolveMensagemIntactaEListaVazia() {
    ResultadoAplicacao r = new MotorRegras(emptyList()).aplicar(new Mensagem().adicionar("00001","saque"), Sentido.REQUISICAO);
    assertFalse(r.houveAdulteracao());
    assertEquals(Optional.of("saque"), r.mensagem().primeiro("00001"));
}
@Test void regrasAplicadasNaOrdemESegundaVeOEstadoDaPrimeira() {
    // r1: SET 00030=100.00 ; r2 condição 00030=~100.00 → ADD 00099=marcado
    // prova que r2 casa sobre o valor que r1 escreveu
}
@Test void naoMutaAMensagemDeEntrada() {
    Mensagem entrada = new Mensagem().adicionar("00001","saque").adicionar("00030","500.00");
    new MotorRegras(singletonList(regra("x", true, Sentido.REQUISICAO,
        new Condicao(singletonMap("00001","saque")), new Acao(Acao.Tipo.SET,"00030","0.01")))).aplicar(entrada, Sentido.REQUISICAO);
    assertEquals(Optional.of("500.00"), entrada.primeiro("00030"));
}
```

- [ ] **Step 2: Rodar e ver falhar** → FAIL.
- [ ] **Step 3: Implementar.** `MotorRegras.aplicar` copia `new ArrayList<>(mensagem.campos())`, percorre as regras ativas do sentido dado, avalia `condicao.casa` sobre uma `Mensagem` reconstruída do estado corrente dos `campos`, aplica cada `Acao` acumulando as adulterações, e devolve `ResultadoAplicacao` com uma `Mensagem` nova reconstruída.
- [ ] **Step 4: Rodar e ver passar** → PASS.
- [ ] **Step 5: Commit** — `feat(interceptador): motor de regras (ordem, sentido, ativa, não destrutivo)`.

---

### Task 4: `CarregadorRegras` (JSON)

**Files:**
- Create: `.../regras/CarregadorRegras.java`, `.../regras/RegrasInvalidasException.java`
- Test: `.../regras/CarregadorRegrasTest.java`
- Test (fixtures): `ccbank-interceptor/src/test/resources/regras-validas.json`, `regras-tipo-invalido.json`

**Interfaces:**
- Consumes: `Regra`, `Condicao`, `Acao`, `Sentido` (Tasks 1–3); Jackson `com.fasterxml.jackson.databind.ObjectMapper`.
- Produces:
  - `final class RegrasInvalidasException extends IOException`: `RegrasInvalidasException(String msg)`.
  - `final class CarregadorRegras`: `static List<Regra> carregar(java.nio.file.Path arquivo) throws IOException`; `static List<Regra> carregar(java.io.InputStream in) throws IOException`.

- [ ] **Step 1: Escrever a fixture `regras-validas.json` e os testes que falham**

```json
[ { "nome": "saque-centavos", "ativa": true, "direcao": "REQUISICAO",
    "quando": { "00001": "saque" },
    "acoes": [ { "tipo": "SET", "rotulo": "00030", "valor": "0.01" } ] },
  { "nome": "sempre-ativa-por-omissao", "direcao": "RESPOSTA",
    "quando": { "00090": "003" },
    "acoes": [ { "tipo": "SET", "rotulo": "00090", "valor": "000" } ] } ]
```

```java
// CarregadorRegrasTest
@Test void carregaNaOrdemComCamposCertos() throws Exception {
    List<Regra> regras = CarregadorRegras.carregar(getClass().getResourceAsStream("/regras-validas.json"));
    assertEquals(2, regras.size());
    assertEquals("saque-centavos", regras.get(0).nome());
    assertEquals(Sentido.REQUISICAO, regras.get(0).sentido());
    assertEquals(Acao.Tipo.SET, regras.get(0).acoes().get(0).tipo());
}
@Test void ativaAusenteAssumeTrue() throws Exception {
    List<Regra> regras = CarregadorRegras.carregar(getClass().getResourceAsStream("/regras-validas.json"));
    assertTrue(regras.get(1).ativa());
}
@Test void jsonMalformadoLancaRegrasInvalidas() {
    assertThrows(RegrasInvalidasException.class, () -> CarregadorRegras.carregar(new ByteArrayInputStream("{nao é json".getBytes(UTF_8))));
}
@Test void tipoDeAcaoInvalidoLancaRegrasInvalidas() {
    assertThrows(RegrasInvalidasException.class, () -> CarregadorRegras.carregar(getClass().getResourceAsStream("/regras-tipo-invalido.json")));
}
@Test void direcaoInvalidaLancaRegrasInvalidas() { /* "direcao":"XPTO" → RegrasInvalidasException */ }
@Test void regexInvalidaNoQuandoLancaRegrasInvalidas() { /* "quando":{"00030":"~("} → RegrasInvalidasException */ }
```

- [ ] **Step 2: Rodar e ver falhar** → FAIL.
- [ ] **Step 3: Implementar `CarregadorRegras`** com `ObjectMapper.readTree`: raiz deve ser array; por nó, lê `nome` (obrigatório), `ativa` (default `true`), `direcao`→`Sentido.porNome`, `quando`→`Map<String,String>` (valores como texto), `acoes[]`→`Acao` (`tipo`→`Acao.Tipo.valueOf`, `rotulo`, `valor`; `valor` dispensável em `REMOVE`). Qualquer falha (JSON inválido, campo faltando, `tipo`/`direcao` desconhecidos, regex inválida vinda de `new Condicao`) é embrulhada em `RegrasInvalidasException` com mensagem clara.
- [ ] **Step 4: Rodar e ver passar** → PASS.
- [ ] **Step 5: Commit** — `feat(interceptador): carregamento de regras em JSON`.

---

### Task 5: `FormatadorAdulteracao` (marcação no log)

**Files:**
- Create: `.../proxy/FormatadorAdulteracao.java`
- Test: `.../proxy/FormatadorAdulteracaoTest.java`

**Interfaces:**
- Consumes: `Mensagem`, `Campo`, `Rotulo` (protocolo); `Adulteracao` (Task 2).
- Produces: `final class FormatadorAdulteracao`: `static String formatar(Mensagem m, List<Adulteracao> adulteracoes)`.

Formata como `FormatadorLog.formatarCampos`, acrescentando a marca ao campo adulterado. Para `SET`: `<antes>  →  <depois>   [ADULTERADO: regra "<nome>"]`. Para `ADD` (antes nulo): `<depois>   [ADICIONADO: regra "<nome>"]`. Para `REMOVE` (depois nulo), que não aparece na mensagem final, uma linha extra ao fim: `<rotulo> <nome> ... (removido)   [REMOVIDO: regra "<nome>"]`. Rótulo repetido é marcado pela última adulteração daquele rótulo.

- [ ] **Step 1: Escrever os testes que falham**

```java
@Test void campoNaoAdulteradoApareceSemMarca() {
    String s = FormatadorAdulteracao.formatar(new Mensagem().adicionar("00030","150.00"), emptyList());
    assertTrue(s.contains("150.00"));
    assertFalse(s.contains("ADULTERADO"));
}
@Test void setMostraAntesDepoisERegra() {
    String s = FormatadorAdulteracao.formatar(new Mensagem().adicionar("00030","1.50"),
        singletonList(new Adulteracao("00030","150.00","1.50","saque-centavos")));
    assertTrue(s.contains("150.00"));
    assertTrue(s.contains("→"));
    assertTrue(s.contains("1.50"));
    assertTrue(s.contains("[ADULTERADO: regra \"saque-centavos\"]"));
}
@Test void addMostraAdicionado() {
    String s = FormatadorAdulteracao.formatar(new Mensagem().adicionar("00099","x"),
        singletonList(new Adulteracao("00099", null, "x", "r")));
    assertTrue(s.contains("[ADICIONADO: regra \"r\"]"));
}
@Test void removeMostraLinhaRemovida() {
    String s = FormatadorAdulteracao.formatar(new Mensagem(),
        singletonList(new Adulteracao("00030","150.00", null, "r")));
    assertTrue(s.contains("(removido)"));
    assertTrue(s.contains("[REMOVIDO: regra \"r\"]"));
}
```

- [ ] **Step 2: Rodar e ver falhar** → FAIL.
- [ ] **Step 3: Implementar `FormatadorAdulteracao`.** Indexa as adulterações por rótulo; percorre `m.campos()` reaproveitando o layout de `FormatadorLog` (recuo, nome legível via `Rotulo.porCodigo`) e anexa a marca; ao fim, emite as linhas de `REMOVE`.
- [ ] **Step 4: Rodar e ver passar** → PASS.
- [ ] **Step 5: Commit** — `feat(interceptador): formatação de log com marcação de adulteração`.

---

### Task 6: Proxy — `ParDeConexoes` e `ServidorProxy`

**Files:**
- Create: `.../proxy/ParDeConexoes.java`, `.../proxy/ServidorProxy.java`
- Test: `.../proxy/ServidorProxyTest.java` (sockets locais em porta efêmera, sem banco)

**Interfaces:**
- Consumes: `MotorRegras`, `ResultadoAplicacao` (Task 3); `FormatadorAdulteracao` (Task 5); `LadoInterceptador` (Task 1); `CodecMensagem` (`lerQuadro(InputStream)→byte[]|null`, lança `MensagemMuitoGrandeException`/`MensagemMalformadaException`; `decodificar(byte[])→Mensagem`; `codificar(Mensagem)→byte[]`); `FormatadorLog`, `Direcao`.
- Produces:
  - `final class ServidorProxy`: `ServidorProxy(int portaEscuta, String hostDestino, int portaDestino, MotorRegras motor, String sufixoLog)`; `static ServidorProxy doLado(LadoInterceptador lado, MotorRegras motor)`; `void iniciar() throws IOException`; `int portaLocal()`; `void parar()`. (Mesmo molde de `ServidorTcp`: thread aceitadora, pool `newCachedThreadPool` de daemons, conjunto de sockets abertos, `parar()` fecha tudo.)
  - `final class ParDeConexoes` (package-private): `ParDeConexoes(Socket cliente, String hostDestino, int portaDestino, MotorRegras motor, String sufixoLog)`; `void executar()`.

Loop de `ParDeConexoes.executar()`: abre socket ao destino; repete — lê quadro do cliente (`lerQuadro`), repassa adulterando no sentido `REQUISICAO` ao servidor; lê quadro do servidor, repassa adulterando no sentido `RESPOSTA` ao cliente — até uma ponta fechar (`null`). `MensagemMuitoGrandeException` → loga a derrubada e encerra o par. O repasse: tenta `decodificar(quadro)`; se `MensagemMalformadaException`, modo opaco (loga o quadro em hex, escreve os **bytes originais**); senão aplica o motor, loga as duas etapas (recebida com a mensagem interpretada, enviada com `FormatadorAdulteracao`) e escreve `codificar(resultado.mensagem())` **se houve adulteração**, ou os **bytes originais** se não houve.

- [ ] **Step 1: Escrever os testes que falham** (helper: servidor-eco em `ServerSocket(0)` numa thread, que lê um quadro e devolve uma resposta configurada; cliente = `Socket` cru)

```java
@Test void passagemEntregaBytesIdenticos() throws Exception {
    // eco devolve 00090=000; proxy sem regras; cliente envia login
    // asserta: servidor recebeu os bytes exatos do cliente; cliente recebeu os bytes exatos do eco
}
@Test void passagemPreservaCrlf() throws Exception {
    // cliente envia quadro com linhas terminadas em CRLF, sem regras
    // asserta: o servidor-eco recebeu os bytes idênticos (sem normalização para LF)
}
@Test void regraSetNaRequisicaoChegaAdulteradaAoServidor() throws Exception {
    // regra REQUISICAO: 00001=saque → SET 00030=0.01 ; cliente envia 00030=500.00
    // asserta: servidor-eco recebeu 00030=0.01
}
@Test void regraNaRespostaChegaAdulteradaAoCliente() throws Exception {
    // eco devolve 00090=003 ; regra RESPOSTA: 00090=003 → SET 00090=000
    // asserta: cliente recebeu 00090=000
}
@Test void quadroNaoDecodificavelPassaEmModoOpaco() throws Exception {
    // cliente envia "sem-igual\n\n" (linha sem '=') ; sem regras
    // asserta: servidor-eco recebeu os mesmos bytes
}
@Test void quadroAcimaDe64kDerrubaOPar() throws Exception {
    // cliente envia > 65536 bytes sem linha vazia
    // asserta: a conexão do cliente é encerrada (read() == -1 após o proxy derrubar)
}
```

- [ ] **Step 2: Rodar e ver falhar** → FAIL.
- [ ] **Step 3: Implementar `ParDeConexoes` e `ServidorProxy`** conforme o loop acima; portas efêmeras nos testes (`ServidorProxy` com `portaEscuta=0`, destino = porta do eco).
- [ ] **Step 4: Rodar e ver passar** — `mvn -B -pl ccbank-interceptor test` → PASS.
- [ ] **Step 5: Commit** — `feat(interceptador): proxy TCP com passagem, regras, modo opaco e limite de 64 KB`.

---

### Task 7: CLI headless — `InterceptadorMain` + logback

**Files:**
- Create: `.../InterceptadorMain.java`
- Create: `ccbank-interceptor/src/main/resources/logback.xml`
- Create: `ccbank-interceptor/src/test/resources/logback-test.xml` (console apenas, para os testes não gravarem em `logs/`)
- Test: `.../InterceptadorMainTest.java`

**Interfaces:**
- Consumes: `LadoInterceptador`, `CarregadorRegras`, `MotorRegras`, `ServidorProxy`.
- Produces: `final class InterceptadorMain`: `public static void main(String[] args)`; `static int executar(String[] args)` (0 = subiu; 1 = falhou, com o motivo no log e no console).

`executar`: valida `--headless` presente e `--lado=esquerdo|direito`; resolve o lado; se `--regras=<arquivo>`, carrega (`CarregadorRegras.carregar(Path)`), senão motor vazio (passagem); define `System.setProperty("ccbank.lado", lado.sufixoLog())` **antes** do primeiro logger; sobe `ServidorProxy.doLado(lado, motor)`, registra shutdown hook para `parar()`, loga "Interceptador <lado> escutando <porta> → <destino> (modo <passagem|regras>, <N> regras)" e retorna 0. Qualquer exceção → loga e retorna 1. A ordem (validar args → carregar regras → abrir porta) garante que os testes de falha não cheguem a abrir a 8001/8002.

- [ ] **Step 1: Escrever o `logback.xml`, o `logback-test.xml` e os testes que falham**

```java
// InterceptadorMainTest  (só caminhos de falha; não abre a porta real)
@Test void semHeadlessRetorna1() { assertEquals(1, InterceptadorMain.executar(new String[]{"--lado=esquerdo"})); }
@Test void semLadoRetorna1() { assertEquals(1, InterceptadorMain.executar(new String[]{"--headless"})); }
@Test void ladoInvalidoRetorna1() { assertEquals(1, InterceptadorMain.executar(new String[]{"--headless","--lado=meio"})); }
@Test void arquivoDeRegrasInexistenteRetorna1() {
    assertEquals(1, InterceptadorMain.executar(new String[]{"--headless","--lado=esquerdo","--regras=nao-existe.json"}));
}
```

- [ ] **Step 2: Rodar e ver falhar** → FAIL.
- [ ] **Step 3: Implementar `InterceptadorMain`** e os dois `logback`. O `logback.xml` usa `ARQUIVO_LOG = interceptor-${ccbank.lado:-esquerdo}`, `RollingFileAppender` para `logs/${ARQUIVO_LOG}.log` (diário, `maxHistory` 30, UTF-8), um `CONSOLE` e `root` INFO com os dois; o `logback-test.xml` tem só `CONSOLE`.
- [ ] **Step 4: Rodar e ver passar** → PASS. `mvn -B clean verify -Pintegracao` verde no reator inteiro.
- [ ] **Step 5: Commit** — `feat(interceptador): CLI headless e logs por lado`.

---

### Task 8: Robustez de teste — `ConexaoServidor.nomeDoServico` e limpeza do `ClienteBancoIT`

**Files:**
- Modify: `ccbank-client/src/main/java/br/com/ccbank/cliente/ConexaoServidor.java` (extrair a escolha do nome do serviço)
- Create: `ccbank-client/src/test/java/br/com/ccbank/cliente/ConexaoServidorTest.java`
- Modify: `ccbank-tests/src/test/java/br/com/ccbank/testes/ClienteBancoIT.java` (remover `portaDeInterceptadorDaMensagemDeInterceptador`)

**Interfaces:**
- Consumes: `LadoCliente` (`portaInterceptador()`).
- Produces: `ConexaoServidor` ganha `static String nomeDoServico(LadoCliente lado, int porta)` (retorna `"Interceptador"` se `porta == lado.portaInterceptador()`, senão `"Servidor"`), usado na montagem da mensagem de `ServicoIndisponivelException`. Saída idêntica à atual (refactor que preserva o comportamento; não altera nada vulnerável do v1).

- [ ] **Step 1: Escrever o teste de unidade que falha**

```java
// ConexaoServidorTest  (sem rede)
@Test void portaDeInterceptadorNomeiaInterceptador() {
    assertEquals("Interceptador", ConexaoServidor.nomeDoServico(LadoCliente.ESQUERDO, 8001));
    assertEquals("Interceptador", ConexaoServidor.nomeDoServico(LadoCliente.DIREITO, 8002));
}
@Test void portaDeServidorNomeiaServidor() {
    assertEquals("Servidor", ConexaoServidor.nomeDoServico(LadoCliente.ESQUERDO, 9001));
    assertEquals("Servidor", ConexaoServidor.nomeDoServico(LadoCliente.DIREITO, 9002));
}
```

- [ ] **Step 2: Rodar e ver falhar** — `mvn -B -pl ccbank-client test` → FAIL (método inexistente).
- [ ] **Step 3: Extrair `nomeDoServico`** em `ConexaoServidor` e usá-lo em `conectarSeNecessario` (a linha `String servico = porta == lado.portaInterceptador() ? ...`). Remover o teste `portaDeInterceptadorDaMensagemDeInterceptador` do `ClienteBancoIT` (a lógica passa a ser coberta, de forma determinística, pelo teste de unidade; `servidorForaDoArDaMensagemClara` continua cobrindo o caminho real de conexão recusada com porta efêmera).
- [ ] **Step 4: Rodar e ver passar** — `mvn -B -pl ccbank-client test` → PASS; `mvn -B clean verify -Pintegracao` → suíte inteira verde.
- [ ] **Step 5: Commit** — `refactor(cliente): nome do serviço indisponível em método testável; remove IT dependente da 8001`.

---

## Verificação final

- `mvn -B clean verify -Pintegracao` verde no reator inteiro, com o módulo `ccbank-interceptor` incluído.
- Motor, carregamento e formatação cobertos por teste de unidade; proxy coberto por teste sobre sockets em porta efêmera.
- `InterceptadorMain --headless --lado=esquerdo` sobe em passagem; com `--regras` sobe em modo regras.
- `ClienteBancoIT` não depende mais da porta 8001 estar livre.
