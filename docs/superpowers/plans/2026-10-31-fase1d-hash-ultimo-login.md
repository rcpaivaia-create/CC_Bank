# Fase 1d — Senhas em MD5 e último login cifrado — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** As senhas do operador (00003) e do cliente (00012) passam a trafegar e ser armazenadas como MD5; cada metade do cliente guarda o último login bem-sucedido num arquivo cifrado com AES e o recarrega no campo de usuário ao abrir.

**Architecture:** `SenhaHash` no módulo de protocolo (compartilhado por cliente e testes). Banco com `CHAR(32)` e seed via `md5()` do PostgreSQL; o servidor não muda (compara texto). No cliente, `ClienteBanco` envia os hashes; `CifraArquivo` (chave fixa no código) e `UltimoLogin` cuidam do arquivo por lado; `PainelLado` lê ao abrir e grava após login bem-sucedido.

**Tech Stack:** Java (bytecode 8, JDK 11), `java.security.MessageDigest`, `javax.crypto` (AES/CBC/PKCS5Padding), Swing, Maven 3.9, JUnit 5.11.3, PostgreSQL 18.

**Spec:** `docs/superpowers/specs/2026-10-31-ccbank-laboratorio-design.md` (seções 4.2, 4.5, 5.1, 5.2, 6, 7.3, 8.1 — revisão (2) de 2026-10-31)

## Global Constraints

- Tudo de `CLAUDE.md` vale aqui. Esta é a última mudança no v1 antes do congelamento.
- MD5: hexadecimal **minúsculo**, 32 caracteres, sobre os bytes UTF-8 da senha, sem salt. `123456` → `e10adc3949ba59abbe56e057f20f883e`; `senha123` → `e7d80ffeefa212b7c5c55700e4f7193e`.
- Banco: `operador.senha CHAR(32)`, `conta.senha CHAR(32)`; seed com `md5('senha123')` e `md5('123456')`. `db/v2` continua cópia byte a byte de `db/v1`.
- Servidor: sem mudança de lógica (comparação exata do texto recebido com o guardado).
- Arquivo do último login: `dados/ultimo-login-esquerdo.dat` e `dados/ultimo-login-direito.dat` (relativos ao diretório de trabalho); conteúdo = Base64( IV de 16 bytes ‖ AES/CBC/PKCS5Padding(login em UTF-8) ); chave AES de 128 bits como constante na classe `CifraArquivo`. `dados/` no `.gitignore`.
- O arquivo só é gravado após login com `000`. Ausente ou inválido → campo de usuário vazio, aviso no log do lado, nenhuma mensagem na tela. A senha nunca é gravada.

## Review Focus

1. **Login digitado com espaços ou maiúsculas** é gravado exatamente como foi aceito pelo servidor (o cliente já apara o usuário antes de enviar). → teste em Task 5.
2. **Arquivo do último login adulterado ou truncado** (Base64 inválido, IV incompleto, padding errado): leitura devolve vazio, sem exceção. → teste em Task 4.
3. **Pasta `dados/` inexistente** na primeira gravação: é criada. → teste em Task 4.
4. **Senha com acentos** (`çãé`): MD5 sobre UTF-8, igual ao `md5()` do PostgreSQL com `client_encoding` UTF-8. → teste em Task 1 (valor de referência) e Task 2 (comparação com o banco).
5. **Hash em maiúsculas ou senha em texto puro** enviados por outro cliente: recusados (`001`/`002`). → teste em Task 2.

---

### Task 1: `SenhaHash` no protocolo

**Files:**
- Create: `ccbank-protocol/src/main/java/br/com/ccbank/protocolo/SenhaHash.java`
- Test: `ccbank-protocol/src/test/java/br/com/ccbank/protocolo/SenhaHashTest.java`

**Interfaces:**
- Produces: `static String md5(String senha)` — hexadecimal minúsculo de 32 caracteres; `null` → `IllegalArgumentException`.

- [ ] **Step 1: Escrever os testes que falham**

```java
@Test void valoresConhecidos() {
    assertEquals("e10adc3949ba59abbe56e057f20f883e", SenhaHash.md5("123456"));
    assertEquals("e7d80ffeefa212b7c5c55700e4f7193e", SenhaHash.md5("senha123"));
    assertEquals("d41d8cd98f00b204e9800998ecf8427e", SenhaHash.md5(""));
}
@Test void usaUtf8() { assertEquals("27c4b302cef836223fd435d6fdcf60b2", SenhaHash.md5("çãé")); }
@Test void nuloFalha() { assertThrows(IllegalArgumentException.class, () -> SenhaHash.md5(null)); }
```

- [ ] **Step 2: Rodar e confirmar a falha** — Run: `mvn -B -pl ccbank-protocol test` · Expected: FAIL de compilação.
- [ ] **Step 3: Implementar `SenhaHash`** (`MessageDigest.getInstance("MD5")`, formatação `%02x`).
- [ ] **Step 4: Rodar e confirmar que passa** — Run: `mvn -B clean verify` · Expected: BUILD SUCCESS.
- [ ] **Step 5: Commit** — `feat(protocolo): hash MD5 das senhas`

---

### Task 2: Banco em MD5 e testes do servidor com hash

**Files:**
- Modify: `db/v1/001_schema.sql` (`operador.senha CHAR(32)`, `conta.senha CHAR(32)`), `db/v1/002_seed.sql` (`md5('senha123')`, `md5('123456')`); copiar ambos para `db/v2/`
- Modify: `ccbank-tests/.../Requisicoes.java` (`login` e `validarSenha` aplicam `SenhaHash.md5` à senha recebida; novos `loginComSenhaBruta(login, valor)` e `validarSenhaBruta(coop, conta, valor)` que enviam o valor sem hash)
- Modify: `ccbank-tests/.../SetupBancoIT.java` (contagens com `senha = md5('senha123')` / `md5('123456')`)
- Test: `LoginSaldoIT.java`, `ValidarSenhaIT.java` (casos novos)

**Interfaces:**
- Consumes: `SenhaHash.md5` (Task 1).
- Produces: `Requisicoes.loginComSenhaBruta`, `Requisicoes.validarSenhaBruta`.

- [ ] **Step 1: Escrever os testes que falham** (parametrizados por `Ambiente`)

```java
// SetupBancoIT.recriaSchemaComSeed: "senha = md5('senha123')" → 12; "senha = md5('123456')" → 30
// LoginSaldoIT
void senhaEmTextoPuroNaoAutentica → loginComSenhaBruta("rodrigop","senha123"): "001"
void hashMaiusculoNaoAutentica    → loginComSenhaBruta("rodrigop", md5("senha123").toUpperCase()): "001"
void hashGravadoIgualAoDoJava     → select senha from operador where login='rodrigop' == SenhaHash.md5("senha123")
// ValidarSenhaIT
void senhaEmTextoPuroRecusada     → validarSenhaBruta("0001","100001","123456"): "002"
void hashMaiusculoRecusado        → validarSenhaBruta(..., md5("123456").toUpperCase()): "002"
void hashCapturadoAutentica       → validarSenhaBruta("0001","100001","e10adc3949ba59abbe56e057f20f883e"): "000"  // documenta o ataque #12
void acentosIguaisAoBanco         → select md5('çãé') == SenhaHash.md5("çãé")
```

- [ ] **Step 2: Rodar e confirmar a falha** — Run: `mvn -B clean verify -Pintegracao -pl ccbank-tests -am` · Expected: FAIL (compilação dos helpers novos; depois, com eles, os logins válidos falham até o seed usar MD5).
- [ ] **Step 3: Alterar schema, seed (v1 e cópia v2), `Requisicoes`**
- [ ] **Step 4: Rodar e confirmar que passa** — Run: `mvn -B clean verify -Pintegracao` · Expected: BUILD SUCCESS.
- [ ] **Step 5: Commit** — `feat(banco): senhas de operador e conta em MD5`

---

### Task 3: Cliente envia MD5

**Files:**
- Modify: `ccbank-client/.../ClienteBanco.java` (`login` envia `SenhaHash.md5(senha)` em 00003; `validarSenha` envia `SenhaHash.md5(senha)` em 00012)
- Test: `ccbank-tests/.../ClienteBancoIT.java`

- [ ] **Step 1: Escrever/ajustar os testes que falham**

```java
void loginEnviaHash → login("rodrigop","senha123"): sucesso; requisicao 00003 == "e7d80ffeefa212b7c5c55700e4f7193e"
// validacaoEOperacao: validacao().requisicao() 00012 == "e10adc3949ba59abbe56e057f20f883e" (antes: "123456")
```

- [ ] **Step 2: Rodar e confirmar a falha** — Run: `mvn -B clean verify -Pintegracao -pl ccbank-tests -am -Dit.test=ClienteBancoIT` · Expected: FAIL (login recusado com `001` e 00012 em texto puro).
- [ ] **Step 3: Implementar** — `ClienteBanco` usa `SenhaHash.md5`.
- [ ] **Step 4: Rodar e confirmar que passa** — Run: `mvn -B clean verify -Pintegracao` · Expected: BUILD SUCCESS.
- [ ] **Step 5: Commit** — `feat(cliente): senhas enviadas em MD5`

---

### Task 4: `CifraArquivo` e `UltimoLogin`

**Files:**
- Create: `ccbank-client/src/main/java/br/com/ccbank/cliente/CifraArquivo.java`
- Create: `ccbank-client/src/main/java/br/com/ccbank/cliente/UltimoLogin.java`
- Modify: `.gitignore` (`dados/`)
- Test: `ccbank-client/src/test/java/br/com/ccbank/cliente/CifraArquivoTest.java`, `UltimoLoginTest.java`

**Interfaces:**
- Produces:
  - `CifraArquivo`: constante `private static final byte[] CHAVE` (16 bytes); `static String cifrar(String texto)` → Base64(IV ‖ cifrado), IV de `SecureRandom`; `static String decifrar(String base64) throws GeneralSecurityException` (Base64 inválido ou conteúdo curto → `GeneralSecurityException`).
  - `UltimoLogin(Path arquivo)`; `static UltimoLogin doLado(LadoCliente lado)` → `dados/ultimo-login-<sufixoLog>.dat`; `Optional<String> ler()` (ausente/inválido → vazio, aviso no logger `ccbank.cliente.<sufixo>` quando inválido); `void salvar(String login) throws IOException` (cria a pasta; grava UTF-8).

- [ ] **Step 1: Escrever os testes que falham**

```java
// CifraArquivoTest
@Test void idaEVolta() { assertEquals("rodrigop", CifraArquivo.decifrar(CifraArquivo.cifrar("rodrigop"))); }
@Test void ivDiferenteACadaCifragem() { assertNotEquals(CifraArquivo.cifrar("x"), CifraArquivo.cifrar("x")); }
@Test void naoContemOTextoEmClaro() { assertFalse(new String(Base64.getDecoder().decode(CifraArquivo.cifrar("rodrigop")), ISO_8859_1).contains("rodrigop")); }
@Test void conteudoAdulteradoFalha() { → base64 "abc", base64 de 10 bytes, último byte trocado: GeneralSecurityException }
@Test void acentos() { assertEquals("joão", CifraArquivo.decifrar(CifraArquivo.cifrar("joão"))); }

// UltimoLoginTest (@TempDir)
@Test void ausenteDevolveVazio()
@Test void salvaELe() → salvar("ana.lima"); new UltimoLogin(mesmo arquivo).ler() == Optional.of("ana.lima")
@Test void criaPasta() → arquivo em dir/dados/x.dat com dados/ inexistente
@Test void novoLoginSubstitui() → salvar("a"); salvar("b"); ler() == "b"
@Test void arquivoCorrompidoDevolveVazio() → Files.write(arquivo, "lixo") → vazio, sem exceção
@Test void arquivoNaoTemOLoginEmClaro() → conteúdo do arquivo não contém "rodrigop"
@Test void ladosIndependentes() → doLado(ESQUERDO) e doLado(DIREITO) apontam para arquivos diferentes
```

- [ ] **Step 2: Rodar e confirmar a falha** — Run: `mvn -B -pl ccbank-client -am test` · Expected: FAIL de compilação.
- [ ] **Step 3: Implementar `CifraArquivo` e `UltimoLogin`; `.gitignore`**
- [ ] **Step 4: Rodar e confirmar que passa** — Run: `mvn -B clean verify` · Expected: BUILD SUCCESS.
- [ ] **Step 5: Commit** — `feat(cliente): último login por lado em arquivo cifrado com AES`

---

### Task 5: Telas usam o último login

**Files:**
- Modify: `ccbank-client/.../tela/PainelLado.java` (construtor `PainelLado(LadoCliente, UltimoLogin)`; o atual delega com `UltimoLogin.doLado(lado)`; ao criar, preenche o usuário com `ler()`; após login `000`, chama `loginEfetuado(String login)` que grava com `salvar`, registrando falha de gravação só no log)
- Modify: `ccbank-client/.../tela/PainelLogin.java` (`void preencherUsuario(String)`, `String usuarioDigitado()`)
- Test: `ccbank-client/src/test/java/br/com/ccbank/cliente/tela/PainelLadoTest.java`

**Interfaces:**
- Produces: `PainelLado(LadoCliente, UltimoLogin)`; `String usuarioLogin()` (texto do campo de usuário); `void loginEfetuado(String login)` (package-private, chamado no sucesso do login).

- [ ] **Step 1: Escrever os testes que falham** (headless, `@TempDir`)

```java
@Test void preencheUsuarioComUltimoLogin() → arquivo salvo com "ana.lima"; new PainelLado(ESQUERDO, ul).usuarioLogin() == "ana.lima"
@Test void semArquivoCampoVazio() → usuarioLogin() == ""
@Test void loginEfetuadoGrava() → p.loginEfetuado("carla.dias"); ul.ler() == "carla.dias"
@Test void ladosNaoSeMisturam() → arquivos distintos para ESQUERDO e DIREITO; gravar no esquerdo não altera o direito
```

- [ ] **Step 2: Rodar e confirmar a falha** — Run: `mvn -B -pl ccbank-client -am test` · Expected: FAIL de compilação.
- [ ] **Step 3: Implementar**
- [ ] **Step 4: Rodar a suíte completa** — Run: `mvn -B clean verify -Pintegracao` · Expected: BUILD SUCCESS.
- [ ] **Step 5: Verificação do executor** — o jar do cliente abre sem erro; com os bancos manuais recriados (somente com autorização do usuário: `scripts\setup-db.ps1 -Recriar`), conferência visual fica para o usuário.
- [ ] **Step 6: Commit** — `feat(cliente): campo de usuário recarrega o último login de cada lado`
