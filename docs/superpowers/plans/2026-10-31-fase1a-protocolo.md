# Fase 1a — Estrutura Maven + Protocolo + Formatador de Log — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Criar o projeto Maven multi-módulo e o módulo `ccbank-protocol`, com mensagem, codec texto, catálogo de rótulos, códigos de retorno, formatação de valores e formatador de log legível, tudo coberto por testes de unidade.

**Architecture:** POM pai `ccbank-parent` centraliza versões e compilação `--release 8`. O módulo `ccbank-protocol` é uma biblioteca pura (sem rede, sem banco, sem logging) que os demais módulos das fases seguintes vão consumir. Classes estáticas sem estado para codec e formatação; `Mensagem` é uma lista ordenada e mutável de campos.

**Tech Stack:** Java (bytecode 8, compilado com JDK 11), Maven 3.9, JUnit 5.11.

**Spec:** `docs/superpowers/specs/2026-10-31-ccbank-laboratorio-design.md` (seções 2, 3.2, 4.1–4.3, 6 e 9.1)

## Global Constraints

- `maven.compiler.release` = `8`; nenhuma API posterior ao Java 8.
- `project.build.sourceEncoding` = `UTF-8` (o encoding padrão da máquina é Cp1252).
- groupId `br.com.ccbank`, versão `0.1.0-SNAPSHOT`, pacote base `br.com.ccbank.protocolo`.
- Protocolo: UTF-8; uma linha `RRRRR=valor` por campo; rótulo = exatamente 5 dígitos; linha quebrada no **primeiro** `=`; linhas terminadas por `\n`; linha vazia encerra a mensagem.
- Mensagem é lista **ordenada**, rótulos podem repetir, rótulos desconhecidos são preservados.
- Limite: **65536 bytes** por mensagem, contando todos os bytes lidos, inclusive as quebras de linha e a linha vazia final.
- Valor monetário: ponto decimal, até duas casas, formatado sempre com duas (`150.00`). Data `AAAA-MM-DD`; data-hora `AAAA-MM-DDTHH:MM:SS` (sem frações de segundo).
- Nomes legíveis e códigos de retorno exatamente como nas tabelas 4.2 e 4.3 da spec.
- Versões no `dependencyManagement` do pai (usadas nas fases seguintes): postgresql `42.7.4`, HikariCP `4.0.3`, slf4j-api `2.0.16`, logback-classic `1.3.14`, jackson-databind `2.17.2`, HdrHistogram `2.2.2`, junit-jupiter `5.11.3`. Plugins: maven-compiler-plugin `3.13.0`, maven-surefire-plugin `3.5.2`.

## Review Focus

1. **Quebra de linha CRLF** (mensagem escrita por ferramenta Windows): o leitor remove um `\r` final de cada linha; a linha `"\r"` sozinha conta como linha vazia. → teste em Task 3.
2. **Bytes que não são UTF-8 válido**: devem gerar `MensagemMalformadaException`, nunca texto com `�`. → teste em Task 3.
3. **Fronteira do limite**: mensagem com exatamente 65536 bytes é aceita; com 65537 é rejeitada com `MensagemMuitoGrandeException`, sem ler o resto. → teste em Task 3.
4. **Acentos** (`Maria Conceição`): ida e volta sem perda, e o limite conta bytes, não caracteres. → teste em Task 3.
5. **Valor digitado no padrão brasileiro** (`150,00`, `1.500,00`) e valor vazio (`00030=`): o codec aceita o campo vazio; `FormatoValores` rejeita vírgula e vazio. → testes em Tasks 3 e 4.

---

### Task 1: Projeto Maven + catálogos `Rotulo` e `CodigoRetorno`

**Files:**
- Create: `pom.xml` (pai, `packaging pom`, módulo `ccbank-protocol`)
- Create: `.gitattributes` (conteúdo: `* text=auto eol=lf`)
- Create: `ccbank-protocol/pom.xml` (dependência de teste `junit-jupiter`)
- Create: `ccbank-protocol/src/main/java/br/com/ccbank/protocolo/Rotulo.java`
- Create: `ccbank-protocol/src/main/java/br/com/ccbank/protocolo/CodigoRetorno.java`
- Test: `ccbank-protocol/src/test/java/br/com/ccbank/protocolo/RotuloTest.java`
- Test: `ccbank-protocol/src/test/java/br/com/ccbank/protocolo/CodigoRetornoTest.java`

**Interfaces:**
- Produces:
  - `enum Rotulo` com constantes nomeadas em maiúsculas a partir do nome da tabela 4.2 (`TRANSACAO("00001","transacao")`, `OPERADOR("00002","operador")`, …, `ID_TRANSACAO("00094","id_transacao")`); métodos `String codigo()`, `String nome()`, `static Optional<Rotulo> porCodigo(String codigo)`.
  - `enum CodigoRetorno`: `SUCESSO("000","sucesso")`, `OPERADOR_INVALIDO("001","operador ou senha inválidos")`, `SENHA_CONTA_INVALIDA("002","senha da conta inválida")`, `SALDO_INSUFICIENTE("003","saldo insuficiente")`, `CONTA_INEXISTENTE("004","conta inexistente")`, `VALOR_INVALIDO("005","valor inválido")`, `MENSAGEM_MALFORMADA("006","mensagem malformada ou campo obrigatório ausente")`, `CONTA_DESTINO_IGUAL("007","conta destino igual à conta de origem")`, `ERRO_INTERNO("099","erro interno")`; métodos `String codigo()`, `String descricao()`, `static Optional<CodigoRetorno> porCodigo(String)`.

- [ ] **Step 1: Escrever os POMs e os testes que falham**

```java
// RotuloTest
@Test void catalogoTem26Rotulos() { assertEquals(26, Rotulo.values().length); }
@Test void codigosSaoCincoDigitosEUnicos() {
    Set<String> vistos = new HashSet<>();
    for (Rotulo r : Rotulo.values()) {
        assertTrue(r.codigo().matches("\\d{5}"), r.name());
        assertTrue(vistos.add(r.codigo()), "duplicado: " + r.codigo());
    }
}
@Test void buscaPorCodigo() {
    assertEquals(Optional.of(Rotulo.TRANSACAO), Rotulo.porCodigo("00001"));
    assertEquals("cooperativa_operador", Rotulo.porCodigo("00004").get().nome());
    assertEquals("lanc_contraparte", Rotulo.porCodigo("00085").get().nome());
    assertEquals(Optional.empty(), Rotulo.porCodigo("99999"));
    assertEquals(Optional.empty(), Rotulo.porCodigo(null));
}

// CodigoRetornoTest
@Test void catalogoCompleto() {
    assertEquals(9, CodigoRetorno.values().length);
    assertEquals("saldo insuficiente", CodigoRetorno.porCodigo("003").get().descricao());
    assertEquals(CodigoRetorno.CONTA_DESTINO_IGUAL, CodigoRetorno.porCodigo("007").get());
    assertEquals(CodigoRetorno.ERRO_INTERNO, CodigoRetorno.porCodigo("099").get());
    assertEquals(Optional.empty(), CodigoRetorno.porCodigo("123"));
}
```

- [ ] **Step 2: Rodar e confirmar a falha**

Run: `mvn -q -pl ccbank-protocol test`
Expected: FAIL de compilação (`Rotulo` e `CodigoRetorno` não existem).

- [ ] **Step 3: Implementar `Rotulo` e `CodigoRetorno`**

Os 26 rótulos e os 9 códigos copiados das tabelas 4.2 e 4.3 da spec. `porCodigo` usa um `Map` estático preenchido no carregamento do enum.

- [ ] **Step 4: Rodar e confirmar que passa**

Run: `mvn -q -pl ccbank-protocol test`
Expected: BUILD SUCCESS, 4 testes.

- [ ] **Step 5: Commit**

```bash
git add pom.xml .gitattributes ccbank-protocol
git commit -m "feat(protocolo): projeto maven e catálogos de rótulos e códigos de retorno"
```

---

### Task 2: `Mensagem` e `Campo`

**Files:**
- Create: `ccbank-protocol/src/main/java/br/com/ccbank/protocolo/Campo.java`
- Create: `ccbank-protocol/src/main/java/br/com/ccbank/protocolo/Mensagem.java`
- Test: `ccbank-protocol/src/test/java/br/com/ccbank/protocolo/MensagemTest.java`

**Interfaces:**
- Consumes: `Rotulo` (Task 1).
- Produces:
  - `final class Campo` imutável: `Campo(String rotulo, String valor)`, `String rotulo()`, `String valor()`, `equals/hashCode/toString`. Construtor lança `IllegalArgumentException` se o rótulo não casar `\d{5}`, se o valor for `null` ou contiver `\n` ou `\r`.
  - `final class Mensagem`: construtor vazio; `Mensagem adicionar(String rotulo, String valor)` e `Mensagem adicionar(Rotulo rotulo, String valor)` (retornam `this`); `Optional<String> primeiro(String rotulo)`, `Optional<String> primeiro(Rotulo rotulo)`; `List<String> todos(String rotulo)`; `List<Campo> campos()` (lista não modificável, na ordem de inserção); `int tamanho()`; `equals/hashCode` por lista de campos.

- [ ] **Step 1: Escrever os testes que falham**

```java
@Test void preservaOrdemERepeticao() {
    Mensagem m = new Mensagem()
        .adicionar(Rotulo.TRANSACAO, "extrato")
        .adicionar("00081", "2026-10-31T10:00:00")
        .adicionar("00081", "2026-10-31T11:00:00");
    assertEquals(3, m.tamanho());
    assertEquals(Arrays.asList("2026-10-31T10:00:00", "2026-10-31T11:00:00"), m.todos("00081"));
    assertEquals("00001", m.campos().get(0).rotulo());
}
@Test void primeiroDevolveVazioQuandoAusente() {
    assertEquals(Optional.empty(), new Mensagem().primeiro(Rotulo.VALOR));
}
@Test void aceitaRotuloDesconhecidoEValorComIgualEVazio() {
    Mensagem m = new Mensagem().adicionar("54321", "a=b").adicionar("00030", "");
    assertEquals(Optional.of("a=b"), m.primeiro("54321"));
    assertEquals(Optional.of(""), m.primeiro("00030"));
}
@Test void rejeitaCampoInvalido() {
    assertThrows(IllegalArgumentException.class, () -> new Campo("0001", "x"));
    assertThrows(IllegalArgumentException.class, () -> new Campo("0000A", "x"));
    assertThrows(IllegalArgumentException.class, () -> new Campo("00001", "a\nb"));
    assertThrows(IllegalArgumentException.class, () -> new Campo("00001", "a\rb"));
    assertThrows(IllegalArgumentException.class, () -> new Campo("00001", null));
}
@Test void camposNaoModificavel() {
    Mensagem m = new Mensagem().adicionar("00001", "saldo");
    assertThrows(UnsupportedOperationException.class, () -> m.campos().clear());
}
@Test void igualdadePorConteudo() {
    assertEquals(new Mensagem().adicionar("00001", "saldo"), new Mensagem().adicionar("00001", "saldo"));
}
```

- [ ] **Step 2: Rodar e confirmar a falha**

Run: `mvn -q -pl ccbank-protocol test -Dtest=MensagemTest`
Expected: FAIL de compilação (`Mensagem`/`Campo` não existem).

- [ ] **Step 3: Implementar `Campo` e `Mensagem`**

`Mensagem` guarda um `ArrayList<Campo>`; `campos()` devolve `Collections.unmodifiableList`.

- [ ] **Step 4: Rodar e confirmar que passa**

Run: `mvn -q -pl ccbank-protocol test`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add ccbank-protocol
git commit -m "feat(protocolo): mensagem como lista ordenada de campos"
```

---

### Task 3: `CodecMensagem` (codificar e ler do stream)

**Files:**
- Create: `ccbank-protocol/src/main/java/br/com/ccbank/protocolo/CodecMensagem.java`
- Create: `ccbank-protocol/src/main/java/br/com/ccbank/protocolo/MensagemMalformadaException.java` (estende `IOException`)
- Create: `ccbank-protocol/src/main/java/br/com/ccbank/protocolo/MensagemMuitoGrandeException.java` (estende `IOException`)
- Test: `ccbank-protocol/src/test/java/br/com/ccbank/protocolo/CodecMensagemTest.java`

**Interfaces:**
- Consumes: `Mensagem`, `Campo` (Task 2).
- Produces:
  - `static final int TAMANHO_MAXIMO = 65536`
  - `static byte[] codificar(Mensagem m)` — cada campo como `rotulo=valor\n`, seguido de `\n`; UTF-8. Lança `IllegalArgumentException` para mensagem sem campos.
  - `static Mensagem ler(InputStream in) throws IOException` — lê até a linha vazia. Devolve `null` se o stream terminar **antes do primeiro byte** (conexão fechada limpa). Lança `MensagemMalformadaException` para: linha sem `=`, rótulo que não seja 5 dígitos, UTF-8 inválido, fim do stream no meio da mensagem, mensagem sem campos. Lança `MensagemMuitoGrandeException` assim que o total lido passar de `TAMANHO_MAXIMO`.
  - `static Mensagem decodificar(byte[] bytes) throws IOException` — equivalente a `ler(new ByteArrayInputStream(bytes))`; `null` vira `MensagemMalformadaException`.

- [ ] **Step 1: Escrever os testes que falham**

```java
private static byte[] utf8(String s) { return s.getBytes(StandardCharsets.UTF_8); }

@Test void codificaNoFormatoDaSpec() {
    Mensagem m = new Mensagem().adicionar("00001", "login").adicionar("00002", "rodrigop").adicionar("00003", "senha123");
    assertEquals("00001=login\n00002=rodrigop\n00003=senha123\n\n",
        new String(CodecMensagem.codificar(m), StandardCharsets.UTF_8));
}
@Test void idaEVoltaComAcentoIgualERepeticao() throws IOException {
    Mensagem m = new Mensagem().adicionar("00093", "Maria Conceição").adicionar("54321", "a=b=c")
        .adicionar("00081", "x").adicionar("00081", "y").adicionar("00030", "");
    assertEquals(m, CodecMensagem.decodificar(CodecMensagem.codificar(m)));
}
@Test void leDuasMensagensSeguidasDoMesmoStream() throws IOException {
    InputStream in = new ByteArrayInputStream(utf8("00001=saldo\n\n00001=saque\n\n"));
    assertEquals(Optional.of("saldo"), CodecMensagem.ler(in).primeiro("00001"));
    assertEquals(Optional.of("saque"), CodecMensagem.ler(in).primeiro("00001"));
    assertNull(CodecMensagem.ler(in));
}
@Test void aceitaCrlf() throws IOException {
    Mensagem m = CodecMensagem.decodificar(utf8("00001=saldo\r\n00002=rodrigop\r\n\r\n"));
    assertEquals(Optional.of("saldo"), m.primeiro("00001"));
    assertEquals(Optional.of("rodrigop"), m.primeiro("00002"));
}
@Test void rejeitaLinhasInvalidas() {
    assertThrows(MensagemMalformadaException.class, () -> CodecMensagem.decodificar(utf8("00001saldo\n\n")));
    assertThrows(MensagemMalformadaException.class, () -> CodecMensagem.decodificar(utf8("0001=saldo\n\n")));
    assertThrows(MensagemMalformadaException.class, () -> CodecMensagem.decodificar(utf8("abcde=saldo\n\n")));
    assertThrows(MensagemMalformadaException.class, () -> CodecMensagem.decodificar(utf8("\n")));
}
@Test void rejeitaFimNoMeioDaMensagem() {
    assertThrows(MensagemMalformadaException.class, () -> CodecMensagem.decodificar(utf8("00001=saldo\n")));
    assertThrows(MensagemMalformadaException.class, () -> CodecMensagem.decodificar(utf8("00001=sal")));
}
@Test void rejeitaUtf8Invalido() {
    byte[] b = {'0','0','0','0','1','=',(byte)0xC3,(byte)0x28,'\n','\n'};
    assertThrows(MensagemMalformadaException.class, () -> CodecMensagem.decodificar(b));
}
@Test void limiteDeTamanhoContaBytes() throws IOException {
    // "00093=" (6) + valor + "\n" (1) + "\n" (1) = 65536 -> valor com 65528 bytes
    String cabe = repetir("ç", 65528 / 2);                 // 2 bytes por caractere
    assertNotNull(CodecMensagem.decodificar(utf8("00093=" + cabe + "\n\n")));
    String passa = repetir("a", 65529);
    assertThrows(MensagemMuitoGrandeException.class, () -> CodecMensagem.decodificar(utf8("00093=" + passa + "\n\n")));
}
@Test void naoLeAlemDoLimite() {
    InputStream infinito = new InputStream() { public int read() { return 'a'; } };
    assertThrows(MensagemMuitoGrandeException.class, () -> CodecMensagem.ler(infinito));
}
@Test void codificarMensagemVaziaFalha() {
    assertThrows(IllegalArgumentException.class, () -> CodecMensagem.codificar(new Mensagem()));
}
```

(`repetir(String, int)` é um helper local do teste.)

- [ ] **Step 2: Rodar e confirmar a falha**

Run: `mvn -q -pl ccbank-protocol test -Dtest=CodecMensagemTest`
Expected: FAIL de compilação (`CodecMensagem` não existe).

- [ ] **Step 3: Implementar `CodecMensagem`**

Leitura byte a byte acumulando a linha corrente num `ByteArrayOutputStream` e um contador total; ao encontrar `\n`, remove um `\r` final, decodifica com `StandardCharsets.UTF_8.newDecoder()` configurado com `CodingErrorAction.REPORT` (`CharacterCodingException` vira `MensagemMalformadaException`) e quebra no primeiro `=`. O chamador é responsável por passar um stream bufferizado.

- [ ] **Step 4: Rodar e confirmar que passa**

Run: `mvn -q -pl ccbank-protocol test`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add ccbank-protocol
git commit -m "feat(protocolo): codec texto com limite de 64 KB"
```

---

### Task 4: `FormatoValores` (valores, datas)

**Files:**
- Create: `ccbank-protocol/src/main/java/br/com/ccbank/protocolo/FormatoValores.java`
- Test: `ccbank-protocol/src/test/java/br/com/ccbank/protocolo/FormatoValoresTest.java`

**Interfaces:**
- Produces:
  - `static Optional<BigDecimal> parseValorOperacao(String texto)` — aceita somente `\d{1,13}(\.\d{1,2})?` e valor > 0; devolve com escala 2. Vazio para `null`, vazio, vírgula, sinal, mais de duas casas, mais de 13 dígitos inteiros, zero.
  - `static String formatarValor(BigDecimal valor)` — escala 2, `toPlainString`, aceita negativos (`-150.00`).
  - `static Optional<LocalDate> parseData(String texto)` — `AAAA-MM-DD` estrito; vazio se inválido (inclusive `2026-02-30`).
  - `static String formatarData(LocalDate data)`
  - `static String formatarDataHora(LocalDateTime dataHora)` — `yyyy-MM-dd'T'HH:mm:ss`, descarta frações.

- [ ] **Step 1: Escrever os testes que falham**

```java
@Test void valoresValidos() {
    assertEquals(new BigDecimal("150.00"), FormatoValores.parseValorOperacao("150.00").get());
    assertEquals(new BigDecimal("150.00"), FormatoValores.parseValorOperacao("150").get());
    assertEquals(new BigDecimal("0.50"), FormatoValores.parseValorOperacao("0.5").get());
    assertEquals(new BigDecimal("9999999999999.99"), FormatoValores.parseValorOperacao("9999999999999.99").get());
}
@ParameterizedTest
@ValueSource(strings = {"", "0", "0.00", "-10.00", "+10.00", "150,00", "1.500,00", "10.001", "abc", " 10.00", "10.", ".50", "10000000000000.00"})
void valoresInvalidos(String texto) {
    assertEquals(Optional.empty(), FormatoValores.parseValorOperacao(texto));
}
@Test void valorNulo() { assertEquals(Optional.empty(), FormatoValores.parseValorOperacao(null)); }
@Test void formataValor() {
    assertEquals("150.00", FormatoValores.formatarValor(new BigDecimal("150")));
    assertEquals("-150.50", FormatoValores.formatarValor(new BigDecimal("-150.5")));
    assertEquals("0.00", FormatoValores.formatarValor(BigDecimal.ZERO));
}
@Test void datas() {
    assertEquals(LocalDate.of(2026, 10, 31), FormatoValores.parseData("2026-10-31").get());
    assertEquals(Optional.empty(), FormatoValores.parseData("31/10/2026"));
    assertEquals(Optional.empty(), FormatoValores.parseData("2026-02-30"));
    assertEquals(Optional.empty(), FormatoValores.parseData(null));
    assertEquals("2026-10-31", FormatoValores.formatarData(LocalDate.of(2026, 10, 31)));
    assertEquals("2026-10-31T14:03:22",
        FormatoValores.formatarDataHora(LocalDateTime.of(2026, 10, 31, 14, 3, 22, 123_000_000)));
}
```

- [ ] **Step 2: Rodar e confirmar a falha**

Run: `mvn -q -pl ccbank-protocol test -Dtest=FormatoValoresTest`
Expected: FAIL de compilação. (Adicionar `junit-jupiter-params` ao POM do módulo se o artefato `junit-jupiter` não estiver em uso.)

- [ ] **Step 3: Implementar `FormatoValores`**

`parseData` usa `DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT)`.

- [ ] **Step 4: Rodar e confirmar que passa**

Run: `mvn -q -pl ccbank-protocol test`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add ccbank-protocol
git commit -m "feat(protocolo): formatação e validação de valores e datas"
```

---

### Task 5: `FormatadorLog` (mensagens com nomes legíveis)

**Files:**
- Create: `ccbank-protocol/src/main/java/br/com/ccbank/protocolo/Direcao.java`
- Create: `ccbank-protocol/src/main/java/br/com/ccbank/protocolo/FormatadorLog.java`
- Test: `ccbank-protocol/src/test/java/br/com/ccbank/protocolo/FormatadorLogTest.java`

**Interfaces:**
- Consumes: `Mensagem`, `Rotulo`, `CodigoRetorno`.
- Produces:
  - `enum Direcao { ENVIADA("→"), RECEBIDA("←") }` com `String seta()`.
  - `static String formatarCampos(Mensagem m)` — uma linha por campo, separadas por `\n`, sem `\n` final. Cada linha: 4 espaços, rótulo, espaço, coluna do nome (nome + espaço, completado com `.` até 21 caracteres; nome sem rótulo conhecido = `desconhecido`), espaço, valor. No rótulo `00090`, quando o código existir em `CodigoRetorno`, acrescenta ` (<descrição>)`.
  - `static String formatarTroca(String lado, Direcao direcao, String endereco, Mensagem m, Long duracaoMs)` — primeira linha `[<lado>] <ENVIADA|RECEBIDA> <seta> <endereco> (<n> campos)`, mais ` em <ms> ms` quando `duracaoMs != null`; depois `\n` e `formatarCampos(m)`. O timestamp fica a cargo do padrão do Logback de cada aplicação.

- [ ] **Step 1: Escrever os testes que falham**

```java
@Test void formataCamposComColunaAlinhada() {
    Mensagem m = new Mensagem().adicionar("00001", "saque").adicionar("00004", "0001").adicionar("77777", "x");
    assertEquals(
        "    00001 transacao ........... saque\n" +
        "    00004 cooperativa_operador  0001\n" +
        "    77777 desconhecido ........ x",
        FormatadorLog.formatarCampos(m));
}
@Test void codigoDeRetornoGanhaDescricao() {
    assertEquals("    00090 codigo_retorno ...... 000 (sucesso)",
        FormatadorLog.formatarCampos(new Mensagem().adicionar("00090", "000")));
    assertEquals("    00090 codigo_retorno ...... 555",
        FormatadorLog.formatarCampos(new Mensagem().adicionar("00090", "555")));
}
@Test void cabecalhoEnviadaSemTempo() {
    Mensagem m = new Mensagem().adicionar("00001", "saldo");
    assertEquals("[ESQ] ENVIADA → 127.0.0.1:9001 (1 campos)\n    00001 transacao ........... saldo",
        FormatadorLog.formatarTroca("ESQ", Direcao.ENVIADA, "127.0.0.1:9001", m, null));
}
@Test void cabecalhoRecebidaComTempo() {
    Mensagem m = new Mensagem().adicionar("00090", "003");
    assertTrue(FormatadorLog.formatarTroca("ESQ", Direcao.RECEBIDA, "127.0.0.1:9001", m, 46L)
        .startsWith("[ESQ] RECEBIDA ← 127.0.0.1:9001 (1 campos) em 46 ms\n"));
}
```

- [ ] **Step 2: Rodar e confirmar a falha**

Run: `mvn -q -pl ccbank-protocol test -Dtest=FormatadorLogTest`
Expected: FAIL de compilação.

- [ ] **Step 3: Implementar `Direcao` e `FormatadorLog`**

- [ ] **Step 4: Rodar a suíte completa**

Run: `mvn -q clean verify`
Expected: BUILD SUCCESS, todos os testes do módulo passando.

- [ ] **Step 5: Commit**

```bash
git add ccbank-protocol
git commit -m "feat(protocolo): formatador de log com nomes legíveis"
```
