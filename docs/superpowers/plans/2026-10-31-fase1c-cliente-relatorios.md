# Fase 1c — Cliente Swing + Ferramenta de relatórios — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entregar o cliente Swing com as duas metades (esquerda → v1/9001, direita → v2/9002), o liga/desliga do interceptador por metade, logs por lado, e a ferramenta administrativa que gera o relatório HTML com resumo, Intercredis e divergência de origem.

**Architecture:** Módulo `ccbank-client` em três camadas: transporte (`ConexaoServidor`, uma conexão TCP persistente por metade), API sem Swing (`ClienteBanco`, guarda o operador logado e monta as mensagens) e telas Swing (`PainelLado` reutilizado nas duas metades). A ferramenta de relatórios fica em `ccbank-server` (pacote `relatorio`): consulta SQL com filtros e cálculo de divergência, gerador HTML autocontido e uma janela Swing. Testes de integração em `ccbank-tests` contra o `ServidorTcp` real.

**Tech Stack:** Java (bytecode 8, JDK 11), Swing, Maven 3.9, SLF4J + Logback 1.3.14, JUnit 5.11.3, PostgreSQL 18.

**Spec:** `docs/superpowers/specs/2026-10-31-ccbank-laboratorio-design.md` (seções 3.3, 6, 7.4, 8.1, 8.2, 9.2)

## Global Constraints

- Tudo de `CLAUDE.md` vale aqui (Java 8, UTF-8, nomes em português, v1 inseguro de propósito).
- Cliente: janela 1280×800, duas metades iguais e fixas (`GridLayout(1, 2)`); títulos `ESQUERDO · v1 original` e `DIREITO · v2 evoluções`.
- Portas: esquerda 9001 (direto) / 8001 (via interceptador); direita 9002 / 8002. Host `127.0.0.1`, sobrescrevível pela system property `ccbank.host`.
- Timeout de leitura do cliente: **120 s**. Timeout de conexão: 5 s.
- Liga/desliga do interceptador só é alterável com a metade **deslogada**.
- Sair apenas volta à tela de login (não há sessão no servidor).
- Chamadas de rede fora da EDT (`SwingWorker`); erros aparecem na própria metade.
- Mensagem de erro quando o interceptador não responde: `Interceptador não disponível em 127.0.0.1:8001` (ou 8002). Servidor direto fora do ar: `Servidor não disponível em 127.0.0.1:9001` (ou 9002).
- Logs: `logs/client-esquerdo.log` e `logs/client-direito.log` (lados `ESQ`/`DIR` no `FormatadorLog`), `logs/admin.log` para a ferramenta de relatórios. Rotação diária, 30 dias, UTF-8.
- Relatório: `relatorios/relatorio-<v1|v2>-<AAAA-MM-DD-HHMMSS>.html`, autocontido (CSS embutido, sem recursos externos), aberto no navegador padrão.

## Decisões fixadas por este plano

- **Metade direita funcional desde já:** é o mesmo `PainelLado` apontando para 9002. O v2 é cópia do v1 até a fase 3; não há placeholder.
- **Valor digitado:** `EntradaValor.normalizar` converte o formato brasileiro antes do envio: com vírgula → remove pontos e troca vírgula por ponto (`1.500,00` → `1500.00`); sem vírgula e no padrão de milhar `\d{1,3}(\.\d{3})+` → remove os pontos (`1.500` → `1500`); demais textos seguem como digitados (o servidor valida e responde `005`). Espaços nas pontas são removidos.
- **Data digitada no extrato:** aceita `DD/MM/AAAA` ou `AAAA-MM-DD`; vazia = omitida; outra coisa é enviada como digitada (servidor responde `006`).
- **Divergência de origem** (relatório): avaliada para todo tipo exceto `LOGIN`. Divergente quando o `operador_login` não existe no cadastro, ou quando cooperativa, PAC ou terminal da linha diferem (`IS DISTINCT FROM`) do terminal cadastrado do operador.
- **Resumo do relatório:** por tipo, quantidade total e soma de `valor` das linhas com `codigo_retorno = '000'`; totais de recusadas (`codigo_retorno <> '000'`), Intercredis e divergências — sempre sobre as linhas exibidas (após filtros).
- **Escape HTML:** todo texto vindo do banco é escapado (`& < > " '`) no relatório, porque os campos do jornal podem ter sido adulterados.
- **Arquivo de log da JVM:** `logback.xml` do servidor passa a usar `logs/${ccbank.log}.log`, com `ccbank.log` padrão `server-${ccbank.env:-v1}`; a ferramenta de relatórios define `ccbank.log=admin`.

## Review Focus

1. **Servidor reiniciado com o cliente aberto:** a próxima operação falha uma vez com mensagem clara e a seguinte reconecta sozinha, sem reabrir o cliente. → teste em Task 2.
2. **Campo do jornal com HTML/script** (ex.: operador `<script>alert(1)</script>`): o relatório mostra o texto literal, sem executar. → teste em Task 6.
3. **Valor digitado em formato brasileiro** (`1.500,00`, `150,5`, ` 10 `): chega ao servidor como `1500.00`, `150.5`, `10`. → teste em Task 1.
4. **Duas operações disparadas em sequência rápida na mesma metade** (duplo clique): requisições não se intercalam no socket; cada resposta corresponde à sua requisição. → teste em Task 2 (`enviar` sincronizado + botões desabilitados durante a chamada).
5. **Relatório de um dia sem movimento:** gera HTML válido com "Nenhuma operação encontrada" e resumo zerado. → teste em Task 6.

---

### Task 1: Módulo `ccbank-client`, lados e entrada de valores

**Files:**
- Modify: `pom.xml` (módulo `ccbank-client`; `ccbank-client` no `dependencyManagement`)
- Modify: `ccbank-tests/pom.xml` (dependência de teste `ccbank-client`)
- Create: `ccbank-client/pom.xml` (deps: `ccbank-protocol`, `slf4j-api`, `logback-classic`; teste `junit-jupiter`; shade gerando `ccbank-client.jar` com `Main-Class: br.com.ccbank.cliente.ClienteMain`)
- Create: `ccbank-client/src/main/java/br/com/ccbank/cliente/LadoCliente.java`
- Create: `ccbank-client/src/main/java/br/com/ccbank/cliente/EntradaValor.java`
- Test: `ccbank-client/src/test/java/br/com/ccbank/cliente/LadoClienteTest.java`, `EntradaValorTest.java`

**Interfaces:**
- Produces:
  - `enum LadoCliente { ESQUERDO("ESQ", "ESQUERDO · v1 original", 9001, 8001, "esquerdo"), DIREITO("DIR", "DIREITO · v2 evoluções", 9002, 8002, "direito") }` com `String sigla()`, `String titulo()`, `int portaServidor()`, `int portaInterceptador()`, `String sufixoLog()`, `int porta(boolean viaInterceptador)`, `static String host()` (system property `ccbank.host`, padrão `127.0.0.1`).
  - `final class EntradaValor`: `static String normalizar(String digitado)`; `static String normalizarData(String digitada)` (`DD/MM/AAAA` → `AAAA-MM-DD`; vazia → `null`; demais → aparada e devolvida como está).

- [ ] **Step 1: Escrever os testes que falham**

```java
// LadoClienteTest
@Test void portas() {
    assertEquals(9001, LadoCliente.ESQUERDO.porta(false));
    assertEquals(8001, LadoCliente.ESQUERDO.porta(true));
    assertEquals(9002, LadoCliente.DIREITO.porta(false));
    assertEquals(8002, LadoCliente.DIREITO.porta(true));
    assertEquals("DIR", LadoCliente.DIREITO.sigla());
}
@Test void hostPadrao() { assertEquals("127.0.0.1", LadoCliente.host()); }

// EntradaValorTest
@ParameterizedTest @CsvSource({"'1.500,00',1500.00", "'150,5',150.5", "' 10 ',10", "'150.00',150.00",
    "'1.500',1500", "'1.234.567,89',1234567.89", "'abc',abc", "'1.50',1.50", "'',''"})
void normaliza(String digitado, String esperado) { assertEquals(esperado, EntradaValor.normalizar(digitado)); }
@Test void normalizaData() {
    assertEquals("2026-10-31", EntradaValor.normalizarData("31/10/2026"));
    assertEquals("2026-10-31", EntradaValor.normalizarData(" 2026-10-31 "));
    assertNull(EntradaValor.normalizarData("  "));
    assertEquals("31-10-2026", EntradaValor.normalizarData("31-10-2026"));
}
```

- [ ] **Step 2: Rodar e confirmar a falha**

Run: `mvn -B -pl ccbank-client -am test`
Expected: FAIL de compilação.

- [ ] **Step 3: Implementar `LadoCliente` e `EntradaValor`**

- [ ] **Step 4: Rodar e confirmar que passa**

Run: `mvn -B clean verify`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add pom.xml ccbank-client ccbank-tests/pom.xml
git commit -m "feat(cliente): módulo cliente, lados e normalização de entrada"
```

---

### Task 2: Transporte e API do cliente (`ConexaoServidor`, `ClienteBanco`)

**Files:**
- Create: `ccbank-client/src/main/java/br/com/ccbank/cliente/ConexaoServidor.java`
- Create: `ccbank-client/src/main/java/br/com/ccbank/cliente/ServicoIndisponivelException.java` (estende `IOException`)
- Create: `ccbank-client/src/main/java/br/com/ccbank/cliente/Resposta.java`
- Create: `ccbank-client/src/main/java/br/com/ccbank/cliente/LinhaExtrato.java`
- Create: `ccbank-client/src/main/java/br/com/ccbank/cliente/ClienteBanco.java`
- Create: `ccbank-client/src/main/resources/logback.xml`
- Test: `ccbank-tests/src/test/java/br/com/ccbank/testes/ClienteBancoIT.java`

**Interfaces:**
- Consumes: `LadoCliente`, `EntradaValor` (Task 1); `CodecMensagem`, `FormatadorLog`, `Direcao`, `Mensagem`, `Rotulo` (protocolo); `ServidorTcp`, `ProcessadorTransacoes`, `BancoTeste`, `Ambiente` nos testes.
- Produces:
  - `ConexaoServidor(LadoCliente lado, String host, int porta)`; `synchronized Resposta enviar(Mensagem req) throws IOException` — conecta sob demanda (timeout 5 s, leitura 120 s), escreve, lê, mede o tempo, loga requisição e resposta no logger `ccbank.cliente.<sufixoLog>` com `FormatadorLog.formatarTroca(sigla, ENVIADA|RECEBIDA, host:porta, msg, ms)`; em qualquer `IOException` fecha o socket (a próxima chamada reconecta) e relança; falha ao conectar → `ServicoIndisponivelException` com a mensagem fixada nas Global Constraints (servidor ou interceptador, conforme a porta seja a do interceptador); servidor fechou a conexão (`ler` devolveu `null`) → `IOException("Conexão encerrada pelo servidor")`. `void fechar()`; `String endereco()`.
  - `Resposta`: `Mensagem mensagem()`, `String codigo()` (`00090`, ou `""` se ausente), `String texto()` (`00091`), `boolean sucesso()` (`"000"`), `long duracaoMs()`, `Mensagem requisicao()`.
  - `LinhaExtrato` (dataHora, tipo, valor, saldoApos, contraparte — todos `String`, como vieram).
  - `ClienteBanco(LadoCliente lado, boolean viaInterceptador)` (cria a `ConexaoServidor` com `LadoCliente.host()` e `lado.porta(via)`); construtor alternativo `ClienteBanco(LadoCliente lado, ConexaoServidor conexao)` para testes. Métodos: `Resposta login(String operador, String senha)` — em sucesso guarda operador, nome, cooperativa, PAC, terminal; `boolean logado()`; `String operador()`, `nomeOperador()`, `cooperativa()`, `pac()`, `terminal()`; `void sair()` (limpa o estado e fecha a conexão); `Resposta saldo(coop, conta, senha)`; `Resposta saque(coop, conta, senha, valorDigitado)`; `Resposta deposito(...)`; `Resposta transferencia(coop, conta, senha, coopDestino, contaDestino, valorDigitado)`; `Resposta extrato(coop, conta, senha, inicioDigitado, fimDigitado)`; `static List<LinhaExtrato> linhasExtrato(Mensagem resposta)` (agrupa 00081–00085 na ordem). Operações exigem `logado()` (senão `IllegalStateException`); valores passam por `EntradaValor.normalizar`, datas por `normalizarData` (omitidas se `null`). Cabeçalho de origem = 00002 e 00004–00006 guardados no login.
  - `logback.xml` do cliente: appenders de arquivo `logs/client-esquerdo.log` e `logs/client-direito.log` ligados aos loggers `ccbank.cliente.esquerdo` e `ccbank.cliente.direito` (`additivity=false`), padrão `%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %msg%n`, UTF-8, rotação diária 30 dias; root `WARN` no console.

- [ ] **Step 1: Escrever os testes que falham** (`ClienteBancoIT`, parametrizado por `Ambiente`; `ServidorTcp` na porta 0 sobre `BancoTeste.recriar(amb)`; `ClienteBanco(LadoCliente.ESQUERDO, new ConexaoServidor(LadoCliente.ESQUERDO, "127.0.0.1", porta))`)

```java
void loginGuardaOperador → login("rodrigop","senha123"): sucesso; logado(); cooperativa()="0001"; terminal()="001"; nomeOperador()="Rodrigo P."
void loginInvalidoNaoLoga → login("rodrigop","x"): codigo()="001"; !logado()
void operacaoSemLoginFalha → assertThrows(IllegalStateException, () -> saldo(...))
void saqueComValorBrasileiro → login; saque("0001","100001","123456","1.000,00"): sucesso; mensagem 00092="0.00";
    resposta.requisicao() 00030 = "1000.00"; duracaoMs() >= 0
void extratoAgrupaLinhas → login; deposito "10,00"; extrato com datas vazias: linhasExtrato(...) tamanho 1, tipo "DEPOSITO", valor "10.00"
void extratoComDataBrasileira → extrato("0001","100001","123456","01/01/2020","31/01/2020"): sucesso, 0 linhas
void servidorForaDoArDaMensagemClara → ConexaoServidor numa porta sem servidor: ServicoIndisponivelException com "Servidor não disponível em 127.0.0.1:<porta>"
void portaDeInterceptadorDaMensagemDeInterceptador → ConexaoServidor(LadoCliente.ESQUERDO, "127.0.0.1", 8001) sem nada escutando: mensagem "Interceptador não disponível em 127.0.0.1:8001"
void reconectaAposServidorReiniciar → login; parar servidor; novo ServidorTcp na MESMA porta; 1ª operação lança IOException; 2ª operação sucesso
void chamadasConcorrentesNaoSeMisturam → login; 20 threads chamando saldo em contas diferentes (100001..100010, 200001..200010) na mesma ClienteBanco:
    cada resposta tem 00093 igual ao titular da conta pedida
```

- [ ] **Step 2: Rodar e confirmar a falha**

Run: `mvn -B clean verify -Pintegracao -pl ccbank-tests -am -Dit.test=ClienteBancoIT`
Expected: FAIL de compilação.

- [ ] **Step 3: Implementar `ConexaoServidor`, `ServicoIndisponivelException`, `Resposta`, `LinhaExtrato`, `ClienteBanco`, `logback.xml`**

- [ ] **Step 4: Rodar e confirmar que passa**

Run: `mvn -B clean verify -Pintegracao`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add ccbank-client ccbank-tests
git commit -m "feat(cliente): conexão persistente e API do cliente com logs por lado"
```

---

### Task 3: Telas — janela, metade, login, liga/desliga do interceptador e painel de mensagens

**Files:**
- Create: `ccbank-client/src/main/java/br/com/ccbank/cliente/tela/JanelaPrincipal.java`
- Create: `ccbank-client/src/main/java/br/com/ccbank/cliente/tela/PainelLado.java`
- Create: `ccbank-client/src/main/java/br/com/ccbank/cliente/tela/PainelLogin.java`
- Create: `ccbank-client/src/main/java/br/com/ccbank/cliente/tela/PainelMensagens.java`
- Create: `ccbank-client/src/main/java/br/com/ccbank/cliente/tela/Tarefa.java` (helper de `SwingWorker`)
- Create: `ccbank-client/src/main/java/br/com/ccbank/cliente/ClienteMain.java`
- Create: `scripts/start-cliente.ps1` (UTF-8 com BOM)
- Test: `ccbank-client/src/test/java/br/com/ccbank/cliente/tela/PainelLadoTest.java`

**Interfaces:**
- Consumes: `ClienteBanco`, `Resposta`, `LadoCliente`, `FormatadorLog`.
- Produces:
  - `PainelLado(LadoCliente lado)` (`JPanel`, `BorderLayout`):
    - Norte: título do lado; linha de status `● host:porta` (verde se a última chamada funcionou, vermelho se falhou, cinza antes da primeira); `JCheckBox` "Via interceptador" (habilitado só deslogado; ao mudar, recria o `ClienteBanco` com a nova porta); dados do operador logado (`operador · Coop cccc · PAC ppp · Term ttt`) ou `(não logado)`.
    - Centro: `CardLayout` com os cartões `"login"` (`PainelLogin`) e `"operacoes"` (preenchido na Task 4; nesta task um `JPanel` vazio com o botão Sair).
    - Sul: `PainelMensagens` recolhível (botão `▸ Mensagens` / `▾ Mensagens`), `JTextArea` monoespaçada não editável; cada chamada acrescenta `FormatadorLog.formatarTroca` da requisição e da resposta.
    - Métodos para teste/uso: `boolean logado()`, `JCheckBox checkInterceptador()`, `String cartaoAtual()`, `ClienteBanco cliente()`.
    - `void executar(String descricao, Callable<Resposta> chamada, Consumer<Resposta> aoConcluir)` — roda via `Tarefa` fora da EDT, desabilita os controles da metade durante a chamada, atualiza status e painel de mensagens, mostra a falha (`IOException`) numa `JLabel` de erro da metade.
  - `PainelLogin`: campos usuário e senha (`JPasswordField`), botão Entrar (também com Enter); mensagem de erro em vermelho com `Resposta.texto()`.
  - `JanelaPrincipal` (`JFrame`, título `CC_Bank`, 1280×800, `GridLayout(1, 2)` com `PainelLado(ESQUERDO)` e `PainelLado(DIREITO)`, `EXIT_ON_CLOSE`, fecha as conexões ao sair).
  - `ClienteMain.main`: `UIManager` com o look and feel do sistema, cria `JanelaPrincipal` na EDT.

- [ ] **Step 1: Escrever o teste que falha** (headless: só `JPanel`, sem `JFrame`)

```java
@Test void estadoInicial() {
    PainelLado p = new PainelLado(LadoCliente.ESQUERDO);
    assertFalse(p.logado());
    assertEquals("login", p.cartaoAtual());
    assertTrue(p.checkInterceptador().isEnabled());
    assertFalse(p.checkInterceptador().isSelected());
}
@Test void checkInterceptadorTrocaPorta() {
    PainelLado p = new PainelLado(LadoCliente.DIREITO);
    p.checkInterceptador().doClick();
    assertEquals("127.0.0.1:8002", p.cliente().endereco());
}
```

(`ClienteBanco.endereco()` delega a `ConexaoServidor.endereco()` — acrescentar em `ClienteBanco` nesta task.)

- [ ] **Step 2: Rodar e confirmar a falha**

Run: `mvn -B -pl ccbank-client -am test -Djava.awt.headless=true`
Expected: FAIL de compilação.

- [ ] **Step 3: Implementar as telas, `ClienteMain` e `start-cliente.ps1`**

`start-cliente.ps1`: empacota se `ccbank-client/target/ccbank-client.jar` não existir; executa `javaw -jar` com diretório de trabalho na raiz do projeto (para os logs irem a `logs/`).

- [ ] **Step 4: Rodar e confirmar que passa**

Run: `mvn -B clean verify -Djava.awt.headless=true`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Verificação manual (feita pelo usuário ao fim da fase; o executor só confere que o app abre sem erro no log)**

Com `scripts/start-servidores.ps1` no ar, abrir `scripts/start-cliente.ps1`: as duas metades aparecem; login `rodrigop`/`senha123` em cada metade mostra o operador e troca para o cartão de operações; Sair volta ao login; marcar "Via interceptador" sem interceptador e tentar logar mostra `Interceptador não disponível em 127.0.0.1:8001`; o painel Mensagens mostra a troca; `logs/client-esquerdo.log` recebe as mensagens.

- [ ] **Step 6: Commit**

```bash
git add ccbank-client scripts/start-cliente.ps1
git commit -m "feat(cliente): janela com as duas metades, login e painel de mensagens"
```

---

### Task 4: Telas — operações e extrato

**Files:**
- Create: `ccbank-client/src/main/java/br/com/ccbank/cliente/tela/PainelOperacoes.java`
- Create: `ccbank-client/src/main/java/br/com/ccbank/cliente/tela/FormularioOperacao.java`
- Create: `ccbank-client/src/main/java/br/com/ccbank/cliente/tela/PainelExtrato.java`
- Modify: `PainelLado.java` (cartão `"operacoes"` passa a ser o `PainelOperacoes`)
- Test: `ccbank-client/src/test/java/br/com/ccbank/cliente/tela/PainelOperacoesTest.java`

**Interfaces:**
- Consumes: `PainelLado.executar`, `ClienteBanco`, `LinhaExtrato`.
- Produces:
  - `PainelOperacoes(PainelLado lado)`: barra com botões `Saldo`, `Saque`, `Depósito`, `Transferência`, `Extrato`, `Sair`; área central com o formulário escolhido (`CardLayout`, cartões `saldo`, `saque`, `deposito`, `transferencia`, `extrato`); `String formularioAtual()`.
  - `FormularioOperacao`: campos por operação — Cooperativa, Conta, (Valor), (Cooperativa destino, Conta destino), Senha do cliente (`JPasswordField`); botão Executar; área de resultado: `✔`/`✖` + `Resposta.texto()`, titular (`00093`), saldo (`00092`), `Transação <00094> · <ms> ms`. Senha do cliente é limpa após cada execução.
  - `PainelExtrato`: Cooperativa, Conta, Data início, Data fim (texto livre, `DD/MM/AAAA`), Senha, Executar; `JTable` não editável com colunas `Data/hora`, `Descrição`, `Valor`, `Saldo após`, `Contraparte` a partir de `ClienteBanco.linhasExtrato`; total de lançamentos.

- [ ] **Step 1: Escrever o teste que falha** (headless)

```java
@Test void trocaDeFormulario() {
    PainelOperacoes p = new PainelOperacoes(new PainelLado(LadoCliente.ESQUERDO));
    assertEquals("saldo", p.formularioAtual());
    p.botao("Transferência").doClick();
    assertEquals("transferencia", p.formularioAtual());
    p.botao("Extrato").doClick();
    assertEquals("extrato", p.formularioAtual());
}
```

(`JButton botao(String rotulo)` é exposto para o teste.)

- [ ] **Step 2: Rodar e confirmar a falha**

Run: `mvn -B -pl ccbank-client -am test -Djava.awt.headless=true`
Expected: FAIL de compilação.

- [ ] **Step 3: Implementar `PainelOperacoes`, `FormularioOperacao`, `PainelExtrato`**

- [ ] **Step 4: Rodar e confirmar que passa**

Run: `mvn -B clean verify -Djava.awt.headless=true`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Verificação manual (feita pelo usuário ao fim da fase; o executor só confere que o app abre sem erro no log)**

Logado na metade esquerda: saldo 0001/100001 → `1000.00 · Maria Souza`; saque `1.000,00` com senha `123456` → saldo `0.00`; depósito `50,25` → `50.25`; transferência para 0002/200001 de `10,00`; extrato sem datas mostra os três lançamentos com contraparte `0002/200001` no último; senha errada mostra `✖ senha da conta inválida`. Repetir uma operação na metade direita e conferir que os saldos são independentes (bancos distintos). Ao final, recriar os bancos manuais com `scripts/setup-db.ps1 -Recriar` **somente se o usuário pedir**.

- [ ] **Step 6: Commit**

```bash
git add ccbank-client
git commit -m "feat(cliente): formulários de operações e extrato"
```

---

### Task 5: Consulta do relatório com divergência de origem

**Files:**
- Create: `ccbank-server/src/main/java/br/com/ccbank/servidor/relatorio/FiltroRelatorio.java`
- Create: `ccbank-server/src/main/java/br/com/ccbank/servidor/relatorio/LinhaRelatorio.java`
- Create: `ccbank-server/src/main/java/br/com/ccbank/servidor/relatorio/ConsultaRelatorio.java`
- Test: `ccbank-tests/src/test/java/br/com/ccbank/testes/ConsultaRelatorioIT.java`

**Interfaces:**
- Consumes: `DataSource`, `ProcessadorTransacoes` e `Requisicoes` nos testes.
- Produces:
  - `FiltroRelatorio` (construtor com `LocalDate inicio, LocalDate fim`; setters encadeáveis `cooperativa(String)`, `pac(String)`, `terminal(String)`, `operador(String)`, `tipo(String)`, `somenteIntercredis(boolean)`, `somenteDivergencias(boolean)`, `incluirRecusadas(boolean)` — padrão `true`; getters correspondentes). Filtros de texto `null`/vazio = sem filtro; comparação exata.
  - `LinhaRelatorio`: `long id`, `LocalDateTime dataHora` (America/Sao_Paulo), `tipo`, `operador`, `cooperativa`, `pac`, `terminal` (alegados), `BigDecimal valor` (pode ser `null`), `contaOrigem` (`cccc/nnnnnn` montado de `cooperativa_origem`/`conta_origem`, vazio se ausentes), `contaDestino` (idem), `codigoRetorno`, `boolean intercredis`, `boolean divergente`, `terminalCadastrado` (`cccc/ppp/ttt`, ou `operador inexistente`, ou vazio para `LOGIN`).
  - `ConsultaRelatorio(DataSource ds)`; `List<LinhaRelatorio> consultar(FiltroRelatorio f) throws SQLException` — período por data inteira (`data_hora >= inicio AND data_hora < fim + 1`), ordem `data_hora, id`; divergência calculada no SQL com `LEFT JOIN operador/terminal/pac/cooperativa` pelo `operador_login` (regra em "Decisões fixadas").

- [ ] **Step 1: Escrever os testes que falham** (parametrizados por `Ambiente`, `BancoTeste.recriar` + `ProcessadorTransacoes`; período = hoje)

```java
void listaOperacoesDoDia → saldo + saque + login: 3 linhas em ordem; saque com valor 10.00 e contaOrigem "0001/100001"
void origemCoerenteNaoDiverge → saque com cabeçalho padrão (rodrigop 0001/001/001): divergente=false; terminalCadastrado "0001/001/001"
void origemFalsificadaDiverge → saque com cabeçalho ("rodrigop","0002","001","001"): divergente=true, terminalCadastrado "0001/001/001"
void operadorInexistenteDiverge → saldo com cabeçalho ("fantasma","0001","001","001"): divergente=true, terminalCadastrado "operador inexistente"
void loginNuncaDiverge → login com senha errada de "naoexiste": divergente=false
void filtros → somenteDivergencias(true) traz só as divergentes; incluirRecusadas(false) exclui codigo<>'000';
    somenteIntercredis(true) traz só o saque em 0002/200001; operador("rodrigop") exclui "fantasma"; tipo("SAQUE")
void foraDoPeriodo → FiltroRelatorio(2020-01-01, 2020-01-31): lista vazia
void transferenciaTrazDestino → transferência 0001/100001 → 0002/200001: contaDestino "0002/200001"
```

- [ ] **Step 2: Rodar e confirmar a falha**

Run: `mvn -B clean verify -Pintegracao -pl ccbank-tests -am -Dit.test=ConsultaRelatorioIT`
Expected: FAIL de compilação.

- [ ] **Step 3: Implementar `FiltroRelatorio`, `LinhaRelatorio`, `ConsultaRelatorio`**

- [ ] **Step 4: Rodar e confirmar que passa**

Run: `mvn -B clean verify -Pintegracao`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add ccbank-server ccbank-tests
git commit -m "feat(relatorio): consulta do jornal com filtros e divergência de origem"
```

---

### Task 6: Gerador do relatório HTML

**Files:**
- Create: `ccbank-server/src/main/java/br/com/ccbank/servidor/relatorio/GeradorRelatorioHtml.java`
- Test: `ccbank-server/src/test/java/br/com/ccbank/servidor/relatorio/GeradorRelatorioHtmlTest.java`

**Interfaces:**
- Consumes: `FiltroRelatorio`, `LinhaRelatorio` (Task 5), `CodigoRetorno`, `FormatoValores`.
- Produces: `static String gerar(String ambiente, FiltroRelatorio filtro, List<LinhaRelatorio> linhas, LocalDateTime geradoEm)` — documento HTML5 completo (`<meta charset="utf-8">`, `<style>` embutido, nenhum `src`/`href` externo):
  - cabeçalho: `Relatório de operações · <ambiente>`, período, filtros aplicados, data/hora de geração;
  - resumo: tabela por tipo (quantidade, soma dos valores efetivados), e os totais `Recusadas`, `Intercredis`, `Divergências de origem`;
  - detalhe: tabela com Data/hora, Cooperativa, PAC, Terminal, Operador, Operação, Valor, Conta origem, Conta destino, Retorno (`código – descrição`), Intercredis (`Sim`/vazio), Divergência (`Sim`/vazio). Linha divergente com `class="divergente"` e `title="Terminal cadastrado: <terminalCadastrado>"`; recusada com `class="recusada"`;
  - sem linhas: `<p class="vazio">Nenhuma operação encontrada</p>` no lugar da tabela de detalhe.
  - `static String escapar(String)` — `&`→`&amp;`, `<`→`&lt;`, `>`→`&gt;`, `"`→`&quot;`, `'`→`&#39;`; `null` → `""`.

- [ ] **Step 1: Escrever os testes que falham** (unidade, linhas montadas à mão)

```java
@Test void escapaConteudoDoJornal() {
    String html = gerar("v1", filtroHoje(), lista(linha().operador("<script>alert(1)</script>")), agora());
    assertFalse(html.contains("<script>"));
    assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
}
@Test void divergenteDestacadaComTerminalCadastrado() {
    String html = gerar("v1", filtroHoje(), lista(linha().divergente(true).terminalCadastrado("0001/001/001")), agora());
    assertTrue(html.contains("class=\"divergente\""));
    assertTrue(html.contains("title=\"Terminal cadastrado: 0001/001/001\""));
}
@Test void resumoContaApenasEfetivadasNaSoma() {
    // SAQUE 000 10.00, SAQUE 000 5.50, SAQUE 003 100.00
    String html = gerar(...);
    assertTrue(html.contains("<td>SAQUE</td><td>3</td><td>15.50</td>"));
    assertTrue(html.contains("Recusadas: 1"));
}
@Test void diaSemMovimento() {
    String html = gerar("v2", filtroHoje(), Collections.emptyList(), agora());
    assertTrue(html.contains("Nenhuma operação encontrada"));
    assertTrue(html.contains("Divergências de origem: 0"));
    assertTrue(html.startsWith("<!DOCTYPE html>"));
}
@Test void semRecursosExternos() {
    String html = gerar("v1", filtroHoje(), lista(linha()), agora());
    assertFalse(html.contains("src="));
    assertFalse(html.contains("href="));
}
```

(`linha()` é um builder de teste local que produz `LinhaRelatorio` com valores padrão.)

- [ ] **Step 2: Rodar e confirmar a falha**

Run: `mvn -B -pl ccbank-server -am test -Dtest=GeradorRelatorioHtmlTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL de compilação.

- [ ] **Step 3: Implementar `GeradorRelatorioHtml`**

- [ ] **Step 4: Rodar e confirmar que passa**

Run: `mvn -B clean verify`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add ccbank-server
git commit -m "feat(relatorio): relatório HTML autocontido com resumo e destaques"
```

---

### Task 7: Janela administrativa, log próprio e script

**Files:**
- Create: `ccbank-server/src/main/java/br/com/ccbank/servidor/relatorio/JanelaRelatorios.java`
- Create: `ccbank-server/src/main/java/br/com/ccbank/servidor/relatorio/RelatoriosMain.java`
- Create: `ccbank-server/src/main/java/br/com/ccbank/servidor/relatorio/ArquivoRelatorio.java`
- Modify: `ccbank-server/src/main/resources/logback.xml` (arquivo `logs/${ccbank.log}.log`, padrão `server-${ccbank.env:-v1}`)
- Create: `scripts/relatorios.ps1` (UTF-8 com BOM)
- Test: `ccbank-server/src/test/java/br/com/ccbank/servidor/relatorio/ArquivoRelatorioTest.java`

**Interfaces:**
- Consumes: `ConsultaRelatorio`, `GeradorRelatorioHtml`, `FiltroRelatorio`, `ConfiguracaoBanco`, `Ambiente`, `Caminhos`.
- Produces:
  - `ArquivoRelatorio`: `static String nome(String ambiente, LocalDateTime quando)` → `relatorio-v1-2026-10-31-140322.html`; `static Path salvar(Path pasta, String nome, String html)` (cria a pasta, grava UTF-8).
  - `JanelaRelatorios` (`JFrame` 640×420): ambiente (`v1`/`v2`), Data início e Data fim (`DD/MM/AAAA`, ambas com hoje), Cooperativa, PAC, Terminal, Operador, Operação (combo: Todas, LOGIN, SALDO, SAQUE, DEPOSITO, TRANSFERENCIA, EXTRATO, INVALIDA), caixas "Só Intercredis", "Só divergências de origem", "Incluir recusadas" (marcada); botão Gerar → consulta (fora da EDT) no banco **manual** do ambiente (`Ambiente.banco()`), grava em `relatorios/` (via `Caminhos.localizar("relatorios")` ou `relatorios` na raiz) e abre com `Desktop.getDesktop().browse`; mostra o caminho gerado e erros na própria janela; loga cada geração em `admin.log` (ambiente, filtros, quantidade de linhas, arquivo).
  - `RelatoriosMain.main`: define `ccbank.log=admin` antes do SLF4J, carrega a configuração do banco, abre a janela; falha ao carregar → diálogo de erro e log.

- [ ] **Step 1: Escrever o teste que falha**

```java
@Test void nomeDoArquivo() {
    assertEquals("relatorio-v2-2026-10-31-140322.html",
        ArquivoRelatorio.nome("v2", LocalDateTime.of(2026, 10, 31, 14, 3, 22)));
}
@Test void salvaEmUtf8CriandoPasta(@TempDir Path dir) throws IOException {
    Path f = ArquivoRelatorio.salvar(dir.resolve("relatorios"), "r.html", "<p>ação</p>");
    assertEquals("<p>ação</p>", new String(Files.readAllBytes(f), StandardCharsets.UTF_8));
}
```

- [ ] **Step 2: Rodar e confirmar a falha**

Run: `mvn -B -pl ccbank-server -am test -Dtest=ArquivoRelatorioTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL de compilação.

- [ ] **Step 3: Implementar `ArquivoRelatorio`, `JanelaRelatorios`, `RelatoriosMain`, ajuste do `logback.xml`, `relatorios.ps1`**

`relatorios.ps1`: empacota se o jar do servidor não existir; `javaw -cp ccbank-server/target/ccbank-server.jar br.com.ccbank.servidor.relatorio.RelatoriosMain`, diretório de trabalho na raiz. Acrescentar `relatorios/` já está no `.gitignore`.

- [ ] **Step 4: Rodar a suíte completa**

Run: `mvn -B clean verify -Pintegracao`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Verificação manual (feita pelo usuário ao fim da fase; o executor só confere que o app abre sem erro no log)**

Após as operações da verificação da Task 4 (e uma operação com origem falsificada enviada por `ClienteTeste`/PowerShell com `00004=0002`), abrir `scripts/relatorios.ps1`, gerar o relatório de hoje do v1: o HTML abre no navegador, o resumo bate com as operações, a linha falsificada aparece destacada com o terminal cadastrado na dica, e `logs/admin.log` registra a geração. Conferir que `logs/server-v1.log` continua recebendo o log do servidor.

- [ ] **Step 6: Commit**

```bash
git add ccbank-server scripts/relatorios.ps1
git commit -m "feat(relatorio): janela administrativa, log próprio e script"
```
