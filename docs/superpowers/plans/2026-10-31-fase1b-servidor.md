# Fase 1b — Banco de dados + Servidor + Testes de integração — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Criar os bancos `ccbank_v1`/`ccbank_v2` (e os de teste), o servidor TCP que processa login, saldo, saque, depósito, transferência e extrato gravando jornal e lançamentos, e a suíte de integração que roda contra os dois ambientes.

**Architecture:** Módulo `ccbank-server` com configuração, setup do banco (aplica scripts SQL numerados), repositórios JDBC, um processador que despacha cada transação para uma classe de operação, e o servidor TCP (uma thread por conexão). Módulo `ccbank-tests` com os testes de integração (failsafe, perfil `integracao`), parametrizados por ambiente. O comportamento do v1 é inseguro de propósito: o servidor confia nos campos de operador/cooperativa/PAC/terminal da mensagem.

**Tech Stack:** Java (bytecode 8, JDK 11), Maven 3.9, PostgreSQL 18 (JDBC 42.7.4), HikariCP 4.0.3, SLF4J 2.0.16 + Logback 1.3.14, JUnit 5.11.3, maven-shade-plugin 3.6.0, maven-failsafe-plugin 3.5.2.

**Spec:** `docs/superpowers/specs/2026-10-31-ccbank-laboratorio-design.md` (seções 2, 3.3, 4.4, 4.5, 5, 6, 9.2)

## Global Constraints

- Tudo de `CLAUDE.md` vale aqui: bytecode Java 8, UTF-8, nomes em português, pacote `br.com.ccbank`, **v1 inseguro de propósito** (não validar se o operador existe nem se o terminal confere em saldo/saque/depósito/transferência/extrato).
- Credenciais em `config/db.properties` (chaves `db.host`, `db.porta`, `db.usuario`, `db.senha`). Nunca logar a senha do banco.
- Bancos: `ccbank_v1`, `ccbank_v2` (manual) e `ccbank_v1_teste`, `ccbank_v2_teste` (testes). Scripts em `db/v1/` e `db/v2/` (v2 = cópia exata do v1 nesta fase).
- Portas: v1 = 9001, v2 = 9002.
- Fuso: `America/Sao_Paulo` (sessão do banco via `connectionInitSql`).
- Rótulos e códigos de retorno sempre via `Rotulo` e `CodigoRetorno` de `ccbank-protocol`.
- Tabela 4.4 da spec define campos obrigatórios por transação; tabela 4.3 os códigos.
- Logs em `logs/server-v1.log` / `logs/server-v2.log`, rotação diária, 30 dias, UTF-8, mensagens formatadas com `FormatadorLog` (lado `V1`/`V2`).
- Testes de unidade em `mvn -B clean verify`; integração em `mvn -B clean verify -Pintegracao` (exige PostgreSQL no ar).

## Decisões fixadas por este plano

- **Ordem das validações** (primeira que falhar define o código):
  1. `00001` ausente ou desconhecido → `006`, jornal com tipo `INVALIDA`.
  2. Campo obrigatório ausente (tabela 4.4) ou data do extrato malformada ou `data_inicio > data_fim` → `006`.
  3. `00030` inválido segundo `FormatoValores.parseValorOperacao` → `005`.
  4. Transferência com origem igual ao destino (cooperativa e número) → `007`.
  5. Conta de origem inexistente → `004`.
  6. Senha da conta diferente → `002`.
  7. Conta destino inexistente → `004`.
  8. Saldo insuficiente → `003`.
  Qualquer exceção inesperada → `099` (com stack trace no log).
- **Login:** casa `login` exato (sem trim, sensível a maiúsculas), operador `ativo`, senha exata. Falha → `001`.
- **Jornal:** toda requisição gera exatamente uma linha em `transacao`, inclusive recusadas. Campos recebidos são gravados como texto bruto (`TEXT`), para que valores adulterados não quebrem o insert.
- **Intercredis:** `true` quando a cooperativa da conta (de origem) difere do `00004` recebido.
- **Mensagens de sucesso (`00091`):** `Login efetuado`, `Saldo consultado`, `Saque efetuado`, `Depósito efetuado`, `Transferência efetuada`, `Extrato gerado`. Erros usam `CodigoRetorno.descricao()`.
- **Lançamentos:** descrição `SAQUE` (D), `DEPOSITO` (C), `TRANSFERENCIA ENVIADA` (D), `TRANSFERENCIA RECEBIDA` (C). Contraparte no extrato = `cccc/nnnnnn`, vazio quando não houver.
- **Extrato:** sem datas → últimos 30 dias (hoje − 30 até hoje, inclusive); período por data inteira; no máximo os 200 lançamentos mais recentes, devolvidos em ordem cronológica crescente.
- **Transferência:** trava as duas contas com `SELECT ... FOR UPDATE` em ordem crescente de `id`.
- **Conexão TCP:** `MensagemMalformadaException` → responde `006` e segue na mesma conexão; `MensagemMuitoGrandeException` ou fim do stream no meio → fecha a conexão.

## Review Focus

1. **Transferências cruzadas simultâneas** (A→B e B→A ao mesmo tempo): nenhum deadlock, saldos finais corretos. → teste em Task 5.
2. **Saque exatamente igual ao saldo** deixa `0.00`; saque de saldo + 0.01 → `003`. → teste em Task 4.
3. **Cliente que fecha o socket no meio de uma mensagem**: a thread da conexão termina, o servidor segue atendendo outras conexões. → teste em Task 7.
4. **Valores adulterados enormes nos campos de texto** (ex.: `00004` com 5.000 caracteres): a operação é processada/recusada normalmente e o jornal grava o texto, sem `099`. → teste em Task 4.
5. **Login com espaço ou maiúscula diferente** (`Rodrigop`, `rodrigop `) e operador inativo → `001`. → teste em Task 3.

---

### Task 1: Módulo `ccbank-server` + configuração

**Files:**
- Modify: `pom.xml` (módulos `ccbank-server`, `ccbank-tests`; `pluginManagement` com `maven-shade-plugin` 3.6.0 e `maven-failsafe-plugin` 3.5.2; perfil `integracao` que ativa o failsafe)
- Create: `ccbank-server/pom.xml` (deps: `ccbank-protocol`, `postgresql`, `HikariCP`, `slf4j-api`, `logback-classic`; teste: `junit-jupiter`; shade gerando `ccbank-server.jar` sem `Main-Class`)
- Create: `ccbank-tests/pom.xml` (deps de teste: `ccbank-server`, `junit-jupiter`; sem código main)
- Create: `config/db.properties.example` (já existe no disco, não versionado — só `git add`)
- Create: `ccbank-server/src/main/java/br/com/ccbank/servidor/config/Ambiente.java`
- Create: `ccbank-server/src/main/java/br/com/ccbank/servidor/config/ConfiguracaoBanco.java`
- Test: `ccbank-server/src/test/java/br/com/ccbank/servidor/config/ConfiguracaoBancoTest.java`

**Interfaces:**
- Produces:
  - `enum Ambiente { V1(9001, "ccbank_v1", "ccbank_v1_teste", "db/v1"), V2(9002, "ccbank_v2", "ccbank_v2_teste", "db/v2") }` com `int porta()`, `String banco()`, `String bancoTeste()`, `String pastaScripts()`, `String lado()` (`"V1"`/`"V2"`), `static Ambiente porNome(String)` (aceita `v1`/`V1`; outro valor → `IllegalArgumentException`).
  - `final class ConfiguracaoBanco`: `static ConfiguracaoBanco carregar(Path arquivo) throws IOException`; `static Path arquivoPadrao()` (= system property `ccbank.config` se definida, senão `config/db.properties` relativo ao diretório atual, subindo até 3 diretórios pais até achar); `String urlJdbc(String banco)` → `jdbc:postgresql://<host>:<porta>/<banco>`; `String usuario()`; `String senha()`; `DataSource criarPool(String banco, int tamanhoMaximo)` (HikariCP, `connectionInitSql = "SET TIME ZONE 'America/Sao_Paulo'"`, `autoCommit = true`); `toString()` **sem a senha**.
  - `carregar` lança `IllegalStateException` com mensagem citando a chave quando `db.usuario` ou `db.senha` estiver ausente ou vazio; `db.host` padrão `localhost`, `db.porta` padrão `5432`.

- [ ] **Step 1: Escrever os testes que falham**

```java
@Test void carregaEmontaUrl(@TempDir Path dir) throws IOException {
    Path f = dir.resolve("db.properties");
    Files.write(f, Arrays.asList("db.host=srv", "db.porta=5433", "db.usuario=u", "db.senha=s"), UTF_8);
    ConfiguracaoBanco c = ConfiguracaoBanco.carregar(f);
    assertEquals("jdbc:postgresql://srv:5433/ccbank_v1", c.urlJdbc("ccbank_v1"));
    assertEquals("u", c.usuario());
}
@Test void usaPadroesDeHostEPorta(@TempDir Path dir) throws IOException {
    Path f = dir.resolve("db.properties");
    Files.write(f, Arrays.asList("db.usuario=u", "db.senha=s"), UTF_8);
    assertEquals("jdbc:postgresql://localhost:5432/x", ConfiguracaoBanco.carregar(f).urlJdbc("x"));
}
@Test void exigeUsuarioESenha(@TempDir Path dir) throws IOException {
    Path f = dir.resolve("db.properties");
    Files.write(f, Arrays.asList("db.usuario=u", "db.senha="), UTF_8);
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> ConfiguracaoBanco.carregar(f));
    assertTrue(e.getMessage().contains("db.senha"));
}
@Test void toStringNaoExpoeSenha(@TempDir Path dir) throws IOException {
    Path f = dir.resolve("db.properties");
    Files.write(f, Arrays.asList("db.usuario=u", "db.senha=segredo123"), UTF_8);
    assertFalse(ConfiguracaoBanco.carregar(f).toString().contains("segredo123"));
}
@Test void ambientePorNome() {
    assertEquals(Ambiente.V1, Ambiente.porNome("v1"));
    assertEquals(9002, Ambiente.porNome("V2").porta());
    assertEquals("ccbank_v2_teste", Ambiente.V2.bancoTeste());
    assertThrows(IllegalArgumentException.class, () -> Ambiente.porNome("v3"));
}
```

- [ ] **Step 2: Rodar e confirmar a falha**

Run: `mvn -B -pl ccbank-server -am test`
Expected: FAIL de compilação (`ConfiguracaoBanco`, `Ambiente` não existem).

- [ ] **Step 3: Implementar `Ambiente` e `ConfiguracaoBanco`**

`Properties.load` com `InputStreamReader` UTF-8.

- [ ] **Step 4: Rodar e confirmar que passa**

Run: `mvn -B clean verify`
Expected: BUILD SUCCESS (todos os módulos; `ccbank-tests` sem testes ainda).

- [ ] **Step 5: Commit**

```bash
git add pom.xml ccbank-server ccbank-tests config/db.properties.example
git commit -m "feat(servidor): módulos servidor e testes, configuração do banco"
```

---

### Task 2: Scripts SQL + `SetupBanco` + harness de integração

**Files:**
- Create: `db/v1/001_schema.sql`, `db/v1/002_seed.sql`
- Create: `db/v2/001_schema.sql`, `db/v2/002_seed.sql` (cópias byte a byte dos de `db/v1`)
- Create: `ccbank-server/src/main/java/br/com/ccbank/servidor/dados/SetupBanco.java`
- Create: `scripts/setup-db.ps1`
- Create: `ccbank-tests/src/test/java/br/com/ccbank/testes/BancoTeste.java` (helper)
- Test: `ccbank-tests/src/test/java/br/com/ccbank/testes/SetupBancoIT.java`

**Interfaces:**
- Consumes: `ConfiguracaoBanco`, `Ambiente` (Task 1).
- Produces:
  - `SetupBanco(ConfiguracaoBanco cfg)`; `void criarBancoSeNaoExistir(String banco)` (conecta no banco `postgres`); `void recriarSchema(String banco, Path pastaScripts)` (`DROP SCHEMA public CASCADE; CREATE SCHEMA public;` e aplica `*.sql` da pasta em ordem alfabética, cada arquivo numa transação); `static void main(String[] args)` — uso `SetupBanco [--recriar] [--somente-teste]`: para cada `Ambiente`, cria os bancos que faltarem; aplica scripts num banco **apenas se ele foi criado agora ou se `--recriar`**; `--somente-teste` limita a `bancoTeste()`. Resolve `db/` subindo diretórios como `ConfiguracaoBanco.arquivoPadrao()`.
  - Teste: `final class BancoTeste` com `static DataSource recriar(Ambiente amb)` — garante o banco de teste, recria o schema com os scripts do ambiente e devolve um pool (tamanho 20) reaproveitado por ambiente; `static BigDecimal saldo(DataSource ds, String coop, String conta)`; `static int contar(DataSource ds, String sql)`.

**Schema (`001_schema.sql`)** — copiar exatamente:

```sql
CREATE TABLE cooperativa (
    id SERIAL PRIMARY KEY,
    codigo CHAR(4) NOT NULL UNIQUE,
    nome VARCHAR(100) NOT NULL
);
CREATE TABLE pac (
    id SERIAL PRIMARY KEY,
    cooperativa_id INTEGER NOT NULL REFERENCES cooperativa(id),
    codigo CHAR(3) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    UNIQUE (cooperativa_id, codigo)
);
CREATE TABLE terminal (
    id SERIAL PRIMARY KEY,
    pac_id INTEGER NOT NULL REFERENCES pac(id),
    codigo CHAR(3) NOT NULL,
    UNIQUE (pac_id, codigo)
);
CREATE TABLE operador (
    id SERIAL PRIMARY KEY,
    login VARCHAR(50) NOT NULL UNIQUE,
    senha VARCHAR(50) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    terminal_id INTEGER NOT NULL UNIQUE REFERENCES terminal(id),
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE cliente (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    cpf CHAR(11) NOT NULL UNIQUE
);
CREATE TABLE conta (
    id SERIAL PRIMARY KEY,
    cooperativa_id INTEGER NOT NULL REFERENCES cooperativa(id),
    numero CHAR(6) NOT NULL,
    cliente_id INTEGER NOT NULL UNIQUE REFERENCES cliente(id),
    senha CHAR(6) NOT NULL,
    saldo NUMERIC(15,2) NOT NULL CHECK (saldo >= 0),
    UNIQUE (cooperativa_id, numero)
);
CREATE TABLE transacao (
    id BIGSERIAL PRIMARY KEY,
    data_hora TIMESTAMPTZ NOT NULL DEFAULT now(),
    tipo VARCHAR(20) NOT NULL,
    operador_login TEXT,
    cooperativa TEXT,
    pac TEXT,
    terminal TEXT,
    conta_origem_id INTEGER REFERENCES conta(id),
    cooperativa_origem TEXT,
    conta_origem TEXT,
    conta_destino_id INTEGER REFERENCES conta(id),
    cooperativa_destino TEXT,
    conta_destino TEXT,
    valor NUMERIC(15,2),
    saldo_apos_origem NUMERIC(15,2),
    saldo_apos_destino NUMERIC(15,2),
    codigo_retorno CHAR(3) NOT NULL,
    intercredis BOOLEAN NOT NULL DEFAULT FALSE,
    ip_origem TEXT,
    duracao_ms INTEGER
);
CREATE INDEX ix_transacao_data_hora ON transacao (data_hora);
CREATE TABLE lancamento (
    id BIGSERIAL PRIMARY KEY,
    conta_id INTEGER NOT NULL REFERENCES conta(id),
    transacao_id BIGINT NOT NULL REFERENCES transacao(id),
    data_hora TIMESTAMPTZ NOT NULL DEFAULT now(),
    natureza CHAR(1) NOT NULL CHECK (natureza IN ('C', 'D')),
    descricao VARCHAR(40) NOT NULL,
    valor NUMERIC(15,2) NOT NULL,
    saldo_apos NUMERIC(15,2) NOT NULL
);
CREATE INDEX ix_lancamento_conta_data ON lancamento (conta_id, data_hora);
```

**Seed (`002_seed.sql`)** — valores obrigatórios (os testes dependem deles); o restante é livre:
- Cooperativas: `0001` Cooperativa Alfa, `0002` Cooperativa Beta, `0003` Cooperativa Gama. Cada uma com PACs `001` e `002`; cada PAC com terminais `001` e `002` (12 terminais).
- 12 operadores ativos, senha `senha123`, um por terminal. Obrigatórios: `rodrigop` (Rodrigo P.) em 0001/001/001; `ana.lima` em 0002/001/001. Os outros 10 com logins livres no formato `nome.sobrenome`.
- 30 clientes (CPF `00000000001`…`00000000030`) e 30 contas, todas com senha `123456`. Números: `100001`–`100010` na 0001, `200001`–`200010` na 0002, `300001`–`300010` na 0003. Obrigatórias:

| Cooperativa | Conta | Titular | Saldo |
|---|---|---|---|
| 0001 | 100001 | Maria Souza | 1000.00 |
| 0001 | 100002 | João Pereira | 500.00 |
| 0001 | 100010 | Pedro Lima | 0.00 |
| 0002 | 200001 | Ana Costa | 2000.00 |

Demais contas: saldos livres entre 0 e 10000.

- [ ] **Step 1: Escrever o teste que falha**

```java
class SetupBancoIT {
    @ParameterizedTest @EnumSource(Ambiente.class)
    void recriaSchemaComSeed(Ambiente amb) throws Exception {
        DataSource ds = BancoTeste.recriar(amb);
        assertEquals(3, BancoTeste.contar(ds, "select count(*) from cooperativa"));
        assertEquals(12, BancoTeste.contar(ds, "select count(*) from terminal"));
        assertEquals(12, BancoTeste.contar(ds, "select count(*) from operador where ativo and senha = 'senha123'"));
        assertEquals(30, BancoTeste.contar(ds, "select count(*) from conta where senha = '123456'"));
        assertEquals(new BigDecimal("1000.00"), BancoTeste.saldo(ds, "0001", "100001"));
        assertEquals(new BigDecimal("0.00"), BancoTeste.saldo(ds, "0001", "100010"));
        assertEquals(0, BancoTeste.contar(ds, "select count(*) from transacao"));
    }
    @Test void scriptsV2SaoCopiaDoV1() throws IOException {
        for (String f : new String[] {"001_schema.sql", "002_seed.sql"}) {
            assertArrayEquals(Files.readAllBytes(raiz().resolve("db/v1/" + f)), Files.readAllBytes(raiz().resolve("db/v2/" + f)), f);
        }
    }
}
```

(`raiz()` = diretório do projeto, resolvido subindo a partir de `user.dir` até achar `db/`.)

- [ ] **Step 2: Rodar e confirmar a falha**

Run: `mvn -B clean verify -Pintegracao -pl ccbank-tests -am`
Expected: FAIL de compilação (`SetupBanco`, `BancoTeste` não existem).

- [ ] **Step 3: Escrever os scripts SQL, `SetupBanco` e `BancoTeste`**

Scripts SQL divididos por `;` no fim de linha (os scripts não usam funções nem blocos `$$`).

- [ ] **Step 4: Rodar e confirmar que passa**

Run: `mvn -B clean verify -Pintegracao`
Expected: BUILD SUCCESS, `SetupBancoIT` 3/3.

- [ ] **Step 5: `scripts/setup-db.ps1`**

Parâmetros `-Recriar` e `-SomenteTeste` repassados ao `SetupBanco`. Roda `mvn -B -q -DskipTests package -pl ccbank-server -am` se `ccbank-server/target/ccbank-server.jar` não existir, depois `java -cp ccbank-server/target/ccbank-server.jar br.com.ccbank.servidor.dados.SetupBanco`. Verificar executando `powershell -File scripts/setup-db.ps1` → cria `ccbank_v1` e `ccbank_v2` com seed; rodar de novo → não altera nada. Conferir com `select count(*) from conta` = 30 nos dois.

- [ ] **Step 6: Commit**

```bash
git add db ccbank-server ccbank-tests scripts
git commit -m "feat(servidor): scripts do banco, setup e harness de integração"
```

---

### Task 3: Processador, jornal, login e saldo

**Files:**
- Create: `ccbank-server/src/main/java/br/com/ccbank/servidor/dados/Operador.java`, `Conta.java`, `RegistroJornal.java` (POJOs)
- Create: `ccbank-server/src/main/java/br/com/ccbank/servidor/dados/RepositorioOperadores.java`, `RepositorioContas.java`, `RepositorioJornal.java`
- Create: `ccbank-server/src/main/java/br/com/ccbank/servidor/negocio/ProcessadorTransacoes.java`, `Operacao.java`, `Recusa.java`, `OperacaoLogin.java`, `OperacaoSaldo.java`
- Create: `ccbank-tests/src/test/java/br/com/ccbank/testes/Requisicoes.java` (helper)
- Test: `ccbank-tests/src/test/java/br/com/ccbank/testes/LoginSaldoIT.java`

**Interfaces:**
- Consumes: `BancoTeste` (Task 2), `Mensagem`, `Rotulo`, `CodigoRetorno`, `FormatoValores`.
- Produces:
  - `Operador` (id, login, nome, cooperativa, pac, terminal, ativo); `Conta` (id, cooperativa, numero, titular, senha, saldo `BigDecimal`).
  - `RepositorioOperadores.buscarPorLogin(Connection, String login) : Optional<Operador>` (join até cooperativa/pac/terminal, códigos como texto).
  - `RepositorioContas.buscar(Connection, String coop, String numero) : Optional<Conta>`; `buscarParaAtualizar(Connection, int id) : Conta` (`FOR UPDATE`); `atualizarSaldo(Connection, int id, BigDecimal saldo)`.
  - `RegistroJornal` com setters/campos para todas as colunas de `transacao` exceto `id`/`data_hora`; `RepositorioJornal.inserir(Connection, RegistroJornal) : long` (devolve id).
  - `interface Operacao { Mensagem executar(Connection c, Mensagem req, RegistroJornal jornal) throws Recusa, SQLException; }` — preenche o jornal conforme avança; recusas de negócio lançam `Recusa(CodigoRetorno)`.
  - `ProcessadorTransacoes(DataSource ds)`; `Mensagem processar(Mensagem req, String ipOrigem)`: valida `00001`, cria o `RegistroJornal` (tipo, operador, coop/pac/terminal brutos, ip), abre conexão com `autoCommit=false`, executa a operação; sucesso → insere jornal (`000`), `commit`, devolve resposta com `00090`, `00091`, `00094` acrescentados; `Recusa` → `rollback`, insere jornal com o código em transação própria, devolve `00090`/`00091`/`00094`; exceção → `rollback`, tenta gravar jornal `099`, devolve `099`. Registra `duracao_ms`. A operação pode precisar do id do jornal antes do commit (lançamentos): expor `long registrarJornal(Connection, RegistroJornal)` chamável pela operação, e o processador só insere se a operação não o fez.
  - Tipos do jornal: `LOGIN`, `SALDO`, `SAQUE`, `DEPOSITO`, `TRANSFERENCIA`, `EXTRATO`, `INVALIDA`.
  - Teste: `Requisicoes` com operador fixo `rodrigop`/`0001`/`001`/`001`: `login(String login, String senha)`, `saldo(String coop, String conta, String senha)`, `saque(coop, conta, senha, valor)`, `deposito(coop, conta, senha, valor)`, `transferencia(coop, conta, senha, coopDestino, contaDestino, valor)`, `extrato(coop, conta, senha, String inicio, String fim)` (datas `null` = omitidas); todas devolvem `Mensagem`.

- [ ] **Step 1: Escrever os testes que falham** (`LoginSaldoIT`, parametrizado por `@EnumSource(Ambiente.class)`, com `BancoTeste.recriar(amb)` + `new ProcessadorTransacoes(ds)` em cada teste)

```java
void loginValidoDevolveTerminal(Ambiente amb) {
    Mensagem r = proc.processar(Requisicoes.login("rodrigop", "senha123"), "127.0.0.1");
    assertEquals(Optional.of("000"), r.primeiro(Rotulo.CODIGO_RETORNO));
    assertEquals(Optional.of("0001"), r.primeiro(Rotulo.COOPERATIVA_OPERADOR));
    assertEquals(Optional.of("001"), r.primeiro(Rotulo.PAC));
    assertEquals(Optional.of("001"), r.primeiro(Rotulo.TERMINAL));
    assertEquals(Optional.of("Rodrigo P."), r.primeiro(Rotulo.NOME_OPERADOR));
    assertEquals(1, BancoTeste.contar(ds, "select count(*) from transacao where tipo='LOGIN' and codigo_retorno='000'"));
}
@ParameterizedTest @MethodSource("loginsInvalidos")   // ("rodrigop","errada"), ("Rodrigop","senha123"), ("rodrigop ","senha123"), ("naoexiste","senha123")
void loginInvalido(Ambiente amb, String login, String senha) {
    assertEquals(Optional.of("001"), proc.processar(Requisicoes.login(login, senha), "ip").primeiro(Rotulo.CODIGO_RETORNO));
}
void operadorInativo(Ambiente amb) { /* update operador set ativo=false where login='rodrigop' */ → "001" }
void saldoValido(Ambiente amb) {
    Mensagem r = proc.processar(Requisicoes.saldo("0001", "100001", "123456"), "ip");
    assertEquals(Optional.of("000"), r.primeiro(Rotulo.CODIGO_RETORNO));
    assertEquals(Optional.of("1000.00"), r.primeiro(Rotulo.SALDO));
    assertEquals(Optional.of("Maria Souza"), r.primeiro(Rotulo.TITULAR));
    assertTrue(r.primeiro(Rotulo.ID_TRANSACAO).isPresent());
}
void saldoSenhaErrada(Ambiente amb)       → "002", nenhum 00092 na resposta
void saldoContaInexistente(Ambiente amb)  → ("0001","999999") → "004"
void campoObrigatorioAusente(Ambiente amb)→ saldo sem 00012 → "006"
void transacaoDesconhecida(Ambiente amb)  → 00001=pix → "006", jornal tipo 'INVALIDA'
void semTransacao(Ambiente amb)           → mensagem só com 00002 → "006", jornal tipo 'INVALIDA'
void jornalGravaOrigemComoRecebida(Ambiente amb) {
    // saldo com 00002=fantasma, 00004=9999, 00005=777, 00006=888 → "000" (v1 confia)
    // jornal: operador_login='fantasma', cooperativa='9999', pac='777', terminal='888', intercredis=true
}
void recusaTambemVaiParaOJornal(Ambiente amb) → após saldo com senha errada: 1 linha codigo_retorno='002'
```

- [ ] **Step 2: Rodar e confirmar a falha**

Run: `mvn -B clean verify -Pintegracao -pl ccbank-tests -am -Dit.test=LoginSaldoIT`
Expected: FAIL de compilação.

- [ ] **Step 3: Implementar repositórios, `ProcessadorTransacoes`, `OperacaoLogin`, `OperacaoSaldo`**

Despacho por `Map<String, Operacao>` com chave igual ao valor de `00001`. Campos obrigatórios conferidos pela operação no início (tabela 4.4); `Recusa` carrega o `CodigoRetorno`.

- [ ] **Step 4: Rodar e confirmar que passa**

Run: `mvn -B clean verify -Pintegracao`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add ccbank-server ccbank-tests
git commit -m "feat(servidor): processador de transações com jornal, login e saldo"
```

---

### Task 4: Saque e depósito

**Files:**
- Create: `ccbank-server/src/main/java/br/com/ccbank/servidor/dados/RepositorioLancamentos.java`
- Create: `ccbank-server/src/main/java/br/com/ccbank/servidor/negocio/OperacaoSaque.java`, `OperacaoDeposito.java`
- Modify: `ProcessadorTransacoes.java` (registrar as operações)
- Test: `ccbank-tests/src/test/java/br/com/ccbank/testes/SaqueDepositoIT.java`

**Interfaces:**
- Produces: `RepositorioLancamentos.inserir(Connection, int contaId, long transacaoId, char natureza, String descricao, BigDecimal valor, BigDecimal saldoApos)`.

- [ ] **Step 1: Escrever os testes que falham** (parametrizados por ambiente)

```java
void saqueDebitaEGeraLancamento → saque 0001/100001 "150.00": "000", 00092="850.00"; saldo no banco 850.00;
    1 lançamento D 'SAQUE' 150.00 saldo_apos 850.00; jornal SAQUE valor 150.00 saldo_apos_origem 850.00 intercredis=false
void saqueDoSaldoInteiro      → saque 0001/100002 "500.00": "000", saldo 0.00
void saqueUmCentavoAcima      → saque 0001/100002 "500.01": "003", saldo continua 500.00, 0 lançamentos
void saqueContaZerada         → saque 0001/100010 "0.01": "003"
@ParameterizedTest valoresInvalidos ("0", "-1.00", "150,00", "1.001", "abc", "") → "005", saldo inalterado
void depositoCredita          → deposito 0001/100010 "10.50": "000", 00092="10.50"; lançamento C 'DEPOSITO'
void depositoSenhaErrada      → "002"
void intercredisMarcado       → saque 0002/200001 "10.00" (operador da 0001): jornal intercredis=true
void campoOrigemGiganteNaoQuebra → saque com 00004 = 5000 x "9": "000", jornal cooperativa com 5000 caracteres
```

- [ ] **Step 2: Rodar e confirmar a falha**

Run: `mvn -B clean verify -Pintegracao -pl ccbank-tests -am -Dit.test=SaqueDepositoIT`
Expected: FAIL (`006` para `saque`/`deposito`, que ainda não estão registradas).

- [ ] **Step 3: Implementar `RepositorioLancamentos`, `OperacaoSaque`, `OperacaoDeposito`**

Fluxo: valida campos e valor → `buscar` → senha → `buscarParaAtualizar` → confere saldo → `atualizarSaldo` → `registrarJornal` → `inserir` lançamento.

- [ ] **Step 4: Rodar e confirmar que passa**

Run: `mvn -B clean verify -Pintegracao`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add ccbank-server ccbank-tests
git commit -m "feat(servidor): saque e depósito com lançamentos"
```

---

### Task 5: Transferência

**Files:**
- Create: `ccbank-server/src/main/java/br/com/ccbank/servidor/negocio/OperacaoTransferencia.java`
- Modify: `ProcessadorTransacoes.java`
- Test: `ccbank-tests/src/test/java/br/com/ccbank/testes/TransferenciaIT.java`

- [ ] **Step 1: Escrever os testes que falham** (parametrizados por ambiente)

```java
void transfereEntreCooperativas → 0001/100001 → 0002/200001 "300.00": "000", 00092="700.00";
    saldos 700.00 e 2300.00; lançamentos 'TRANSFERENCIA ENVIADA' D (100001) e 'TRANSFERENCIA RECEBIDA' C (200001), mesma transacao_id;
    jornal: conta_destino_id preenchido, saldo_apos_destino 2300.00
void mesmaConta          → destino 0001/100001: "007"
void destinoInexistente  → destino 0003/999999: "004", saldo origem inalterado
void saldoInsuficiente   → 0001/100002 "500.01": "003"
void senhaErrada         → "002"
void transferenciasCruzadasSemDeadlock → 2 x 50 threads: 50 transferências 100001→100002 "1.00" e 50 100002→100001 "1.00",
    todas "000" em até 30 s; saldos finais 1000.00 e 500.00; 200 lançamentos
```

- [ ] **Step 2: Rodar e confirmar a falha**

Run: `mvn -B clean verify -Pintegracao -pl ccbank-tests -am -Dit.test=TransferenciaIT`
Expected: FAIL (`006` para `transferencia`).

- [ ] **Step 3: Implementar `OperacaoTransferencia`**

Ordem das validações da seção "Decisões fixadas". Trava as duas contas por `buscarParaAtualizar` em ordem crescente de id.

- [ ] **Step 4: Rodar e confirmar que passa**

Run: `mvn -B clean verify -Pintegracao`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add ccbank-server ccbank-tests
git commit -m "feat(servidor): transferência com travamento ordenado"
```

---

### Task 6: Extrato

**Files:**
- Modify: `RepositorioLancamentos.java` (`listar`)
- Create: `ccbank-server/src/main/java/br/com/ccbank/servidor/negocio/OperacaoExtrato.java`
- Modify: `ProcessadorTransacoes.java`
- Test: `ccbank-tests/src/test/java/br/com/ccbank/testes/ExtratoIT.java`

**Interfaces:**
- Produces: `RepositorioLancamentos.listar(Connection, int contaId, LocalDate inicio, LocalDate fim, int limite) : List<LancamentoExtrato>` — os `limite` mais recentes no período, devolvidos em ordem crescente de `data_hora`, `id`; `LancamentoExtrato` (dataHora `LocalDateTime`, descricao, natureza, valor, saldoApos, contraparte `String` — `cccc/nnnnnn` ou vazio).

- [ ] **Step 1: Escrever os testes que falham** (parametrizados por ambiente)

```java
void extratoSemDatasTrazUltimos30Dias → depósito 10.00, saque 5.00, transferência 1.00 para 0002/200001 em 0001/100001;
    extrato sem datas: "000", 00080="3", 00081..00085 repetidos 3 vezes em ordem: 'DEPOSITO' "10.00" "1010.00" "";
    'SAQUE' "-5.00" "1005.00" ""; 'TRANSFERENCIA ENVIADA' "-1.00" "1004.00" "0002/200001"; 00093="Maria Souza"
void extratoForaDoPeriodo  → datas 2020-01-01 a 2020-01-31: "000", 00080="0", nenhum 00081
void limitaA200MaisRecentes → 205 depósitos de 1.00: 00080="200", primeiro 00084 = "1006.00", último 00084 = "1205.00"
void dataMalformada        → 00031="31/10/2026": "006"
void inicioDepoisDoFim     → 2026-10-31 a 2026-10-01: "006"
void extratoNaoGeraLancamento → após extrato, 0 lançamentos novos; jornal tipo EXTRATO "000"
```

(205 depósitos: inserir direto via processador num laço.)

- [ ] **Step 2: Rodar e confirmar a falha**

Run: `mvn -B clean verify -Pintegracao -pl ccbank-tests -am -Dit.test=ExtratoIT`
Expected: FAIL (`006` para `extrato`).

- [ ] **Step 3: Implementar `listar` e `OperacaoExtrato`**

Período em SQL: `data_hora >= ?::date AND data_hora < ?::date + 1`; os 200 mais recentes via subconsulta `ORDER BY data_hora DESC, id DESC LIMIT ?` reordenada ascendente. Contraparte: o outro lançamento com a mesma `transacao_id`.

- [ ] **Step 4: Rodar e confirmar que passa**

Run: `mvn -B clean verify -Pintegracao`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add ccbank-server ccbank-tests
git commit -m "feat(servidor): extrato por período"
```

---

### Task 7: Servidor TCP, logs, main e scripts de execução

**Files:**
- Create: `ccbank-server/src/main/java/br/com/ccbank/servidor/ServidorTcp.java`
- Create: `ccbank-server/src/main/java/br/com/ccbank/servidor/ServidorMain.java`
- Create: `ccbank-server/src/main/resources/logback.xml`
- Create: `scripts/start-servidores.ps1`, `scripts/stop-servidores.ps1`
- Create: `ccbank-tests/src/test/java/br/com/ccbank/testes/ClienteTeste.java` (helper)
- Test: `ccbank-tests/src/test/java/br/com/ccbank/testes/ServidorTcpIT.java`

**Interfaces:**
- Consumes: `ProcessadorTransacoes`, `CodecMensagem` (`ler`, `codificar`), `FormatadorLog`, `Direcao`.
- Produces:
  - `ServidorTcp(int porta, ProcessadorTransacoes proc, String lado)`; `void iniciar() throws IOException` (não bloqueia; aceita conexões numa thread própria, cada conexão num `ExecutorService` de threads); `int portaLocal()`; `void parar()` (fecha o `ServerSocket` e as conexões abertas).
  - Por conexão: `BufferedInputStream`/`BufferedOutputStream`; laço `ler → processar → codificar → flush`; loga cada requisição e resposta com `FormatadorLog.formatarTroca(lado, RECEBIDA|ENVIADA, ip:porta, msg, duracaoMs)` no logger `ccbank.mensagens`.
  - `ServidorMain.main(--env=v1|v2)`: define `System.setProperty("ccbank.env", "v1"|"v2")` **antes** do primeiro uso do SLF4J, carrega `ConfiguracaoBanco.arquivoPadrao()`, pool de 50 conexões no `banco()` do ambiente, inicia na `porta()`, registra `shutdown hook` que chama `parar()`.
  - `logback.xml`: appender de arquivo `logs/server-${ccbank.env:-v1}.log`, `TimeBasedRollingPolicy` diário com `maxHistory=30`, encoder `UTF-8`, padrão `%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %msg%n`; também console.
  - Teste: `ClienteTeste(int porta)` com `Mensagem enviar(Mensagem)`, `void enviarBytes(byte[])`, `Mensagem receber()`, `close()`.

- [ ] **Step 1: Escrever os testes que falham** (parametrizados por ambiente; servidor na porta `0`)

```java
void loginESaldoPelaRede → login + saldo na mesma conexão: "000" e "000", 00092="1000.00"
void mensagemMalformadaResponde006EContinua → enviarBytes("lixo\n\n") → receber() 00090="006"; em seguida saldo na mesma conexão → "000"
void clienteQueCaiNoMeioNaoDerrubaServidor → abre conexão, envia "00001=sal" e fecha; nova conexão: saldo → "000"
void mensagemGrandeFechaConexao → envia 70000 bytes sem linha vazia; receber() devolve null (conexão fechada); nova conexão funciona
void cemSaquesConcorrentesNaMesmaConta → 100 conexões, cada uma saque 0001/100002 "10.00":
    exatamente 50 "000" e 50 "003"; saldo final 0.00; 50 lançamentos; 100 linhas SAQUE no jornal
void somaDosSaldosFecha → após cemSaques: soma(saldos) == soma inicial − 500.00
```

- [ ] **Step 2: Rodar e confirmar a falha**

Run: `mvn -B clean verify -Pintegracao -pl ccbank-tests -am -Dit.test=ServidorTcpIT`
Expected: FAIL de compilação (`ServidorTcp` não existe).

- [ ] **Step 3: Implementar `ServidorTcp`, `ServidorMain`, `logback.xml`**

- [ ] **Step 4: Rodar a suíte completa**

Run: `mvn -B clean verify -Pintegracao`
Expected: BUILD SUCCESS, todos os ITs dos dois ambientes passando.

- [ ] **Step 5: Scripts e verificação manual**

`start-servidores.ps1`: empacota se o jar não existir e abre os servidores v1 e v2 em janelas separadas (`Start-Process java -ArgumentList '-cp', '<jar>', 'br.com.ccbank.servidor.ServidorMain', '--env=v1'`). `stop-servidores.ps1`: encerra os processos `java` cuja linha de comando contém `ServidorMain`. Verificar: iniciar, conectar na 9001 com `ClienteTeste` (ou `Test-NetConnection localhost -Port 9001`), conferir `logs/server-v1.log` com a troca formatada e acentos corretos, parar.

- [ ] **Step 6: Commit**

```bash
git add ccbank-server ccbank-tests scripts
git commit -m "feat(servidor): servidor TCP, logs e scripts de execução"
```
