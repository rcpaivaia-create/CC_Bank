# Fase 2c — Catálogo de ataques, testes de ataque e matriz HTML

**Data:** 2026-11-01
**Status:** rascunho para revisão.
**Spec-mãe (fonte de verdade):** `docs/superpowers/specs/2026-10-31-ccbank-laboratorio-design.md`, seções 7.3 e 9.3.
**Depende de:** interceptador headless (2a) e tela (2b); servidor, cliente de teste e banco das fases 1.

## 1. Objetivo e escopo

Provar, de forma automatizada e reproduzível, que cada ataque do catálogo funciona no v1,
comparar o resultado com uma expectativa por lado, e emitir uma **matriz de ataques** (ataque ×
lado) em HTML. Quando a fase 3 der proteções ao v2, a mesma suíte passa a exigir `PROTEGIDO` no
lado protegido — a infra da 2c é o arcabouço que mede isso.

**Dentro do escopo (2c):**

- Catálogo de regras dos ataques dirigidos pelo interceptador em `regras/ataques-exemplo.json`.
- Harness que, por `Ambiente` (v1/v2), sobe servidor + interceptador (headless) + cliente e roda
  cada ataque; e executores offline/observacional para os que não passam pelo interceptador.
- `expectativas-ataques.properties` com o veredito esperado por ataque e lado; o teste falha se
  o real divergir.
- `GeradorMatrizAtaques` que emite a matriz HTML autocontida.

**Fora do escopo:**

- As proteções do v2 (fase 3) e o benchmark comparativo (fase 3).
- Qualquer mudança no v1 (congelado) ou nas regras de negócio.

## 2. Restrições

Herdadas da spec-mãe seção 2 e dos módulos existentes: bytecode Java 8 (`--release 8` no JDK
11), dependências compatíveis com Java 8 e versões centralizadas no `dependencyManagement` da
raiz, UTF-8 em tudo, scripts em PowerShell. O código vive em `ccbank-tests` (seção 3.2 da
spec-mãe: "testes de integração, de ataque, gerador de carga, relatório comparativo"). Sem
dependências novas. Os testes de ataque rodam sob o perfil `integracao` (PostgreSQL real),
como os demais `*IT`.

## 3. Arquitetura

O harness reusa o padrão dos ITs: `BancoTeste.recriar(amb)` reseta o banco do `Ambiente`,
`ServidorTcp` sobe em porta efêmera, e o `ClienteTeste` fala o protocolo. A novidade é pôr o
`ServidorProxy` (2a) entre o cliente e o servidor, com a regra do ataque carregada.

### 3.1 Componentes

- **`regras/ataques-exemplo.json`** — lista de regras nomeadas (formato da seção 7.2),
  uma por ataque dirigido por regra. É a fonte única: o harness carrega a regra pelo nome com
  `CarregadorRegras.carregar`, e o mesmo arquivo é carregável na tela (2b). Ataques sem regra
  (replay, offline, observacional) não aparecem nele.
- **`Ataque`** — contrato de um ataque: `String id()`, `String descricao()`, e
  `Veredito avaliar(ContextoAtaque contexto)` que roda o cenário para um lado e devolve
  `VULNERAVEL` ou `PROTEGIDO`. Cada ataque do catálogo é uma implementação (ou uma entrada de
  uma lista) com seu cenário e sua checagem de veredito.
- **`Veredito`** — enum `VULNERAVEL`, `PROTEGIDO`.
- **`ContextoAtaque`** — o que um ataque precisa para um lado: o `Ambiente`, o `DataSource`
  recriado, um modo de abrir `(ServidorTcp + ServidorProxy + ClienteTeste através do proxy)`
  com uma regra, e acesso às linhas de log do interceptador daquele lado (para #9) via um
  `ListAppender` do logback ligado ao logger `ccbank.interceptador.<lado>`. Fecha os recursos ao
  final.
- **`expectativas-ataques.properties`** — chaves `<id>.<lado>=VULNERAVEL|PROTEGIDO`
  (`lado` ∈ {v1, v2}). Inicialmente tudo `VULNERAVEL`.
- **`GeradorMatrizAtaques`** — recebe os resultados `(id, lado) → Veredito` e escreve um HTML
  autocontido (CSS embutido, sem dependências externas): linhas = ataques (id + descrição),
  colunas = v1 e v2, células com o veredito destacado (VULNERÁVEL em vermelho, PROTEGIDO em
  verde). Saída em `resultados/matriz-ataques-<AAAA-MM-DD-HHMMSS>.html` (fora do git).
- **`AtaquesIT`** — teste parametrizado por (ataque × lado): roda `avaliar`, compara com a
  expectativa e **falha se divergir**; acumula os resultados num coletor estático e, num passo
  final (`@AfterAll`), chama o `GeradorMatrizAtaques`.

### 3.2 Tipos de executor

- **Dirigido pelo interceptador** (#1–#8, #10, #11, #12): abre servidor + proxy + cliente;
  carrega a regra (quando houver) do catálogo; executa a operação; mede o veredito pela resposta
  recebida e pelo estado do banco.
- **Observacional** (#9): roda uma operação em modo passagem e lê o arquivo de log do
  interceptador do lado, procurando o MD5 da senha.
- **Offline** (#13, #14): sem servidor nem proxy; computa diretamente.

## 4. Catálogo de ataques e veredito

| # | id | Como roda | VULNERÁVEL quando |
|---|---|---|---|
| 1 | valor-adulterado | regra `SET 00030` na requisição de saque de 500.00 | o banco debita o valor adulterado, não os 500.00 |
| 2 | deposito-inflado | regra `SET 00030` no depósito | o banco credita o valor inflado |
| 3 | transferencia-desviada | regra `SET` na conta destino | a conta trocada é creditada |
| 4 | origem-falsificada | regra `SET` em cooperativa/PAC/terminal | o jornal grava a origem forjada (divergência) |
| 5 | operador-falsificado | regra `SET 00002` | o jornal grava outro operador |
| 6 | resposta-falsa | saque com saldo insuficiente + regra `RESPOSTA` `00090` 003→000 | o cliente vê 000 com o banco intacto |
| 7 | saldo-exibido-falso | consulta de saldo + regra `RESPOSTA` `SET 00092` | o cliente vê saldo diferente do real |
| 8 | replay | captura o saque e `Replay.reenviar` N vezes | o banco debita N vezes |
| 9 | captura-passiva | operação em passagem; lê o log do interceptador | o MD5 da senha aparece legível no log |
| 10 | validacao-falsificada | validar_senha com senha errada + regra `RESPOSTA` `00090` 002→000 | o cliente recebe 000 para senha inválida |
| 11 | operacao-sem-validacao | envia o saque sem validar_senha antes | o v1 efetiva a operação mesmo assim |
| 12 | pass-the-hash | captura o MD5 e o reusa como `00003` num novo login | o login é aceito só com o hash capturado |
| 13 | quebra-hash | força bruta no espaço de 6 dígitos sobre `md5('123456')` | recupera o texto puro |
| 14 | chave-aes-no-codigo | decifra um `dados/ultimo-login-*.dat` com a chave do `CifraArquivo` | recupera o login |

Detalhes de veredito que a tabela não fixa:

- **#6**: o saque usa uma conta sem saldo; o servidor responde `003` e não altera o banco; a
  regra na resposta troca para `000`. VULNERÁVEL = o cliente recebeu `000` **e** o saldo no banco
  não mudou (engano puro). PROTEGIDO = o cliente recebe o `003` real.
- **#8**: captura os bytes que foram ao servidor num saque bem-sucedido e reenvia 3 vezes direto
  ao servidor; VULNERÁVEL = o saldo caiu 3 saques além do original. O valor N é fixo no teste.
- **#9**: um `ListAppender` do logback é ligado ao logger `ccbank.interceptador.<lado>` antes da
  operação (em passagem). VULNERÁVEL = alguma linha capturada contém o hash MD5 da senha usada.
  PROTEGIDO = nenhuma contém (mascarado).
- **#11**: envia a transação de saque sem a transação `validar_senha` anterior. VULNERÁVEL = o
  servidor retorna `000` e o banco é alterado. (No v1 a validação é só no cliente, então efetiva.)
- **#12**: faz um login normal (o cliente manda `00003 = md5(senha)`); captura esse valor;
  abre uma nova conexão e envia um login com `00003` = o hash capturado (sem conhecer o texto).
  VULNERÁVEL = retorna `000`.
- **#13**: parte de `md5('123456')` (senha conhecida de conta) e percorre `000000`–`999999`
  calculando MD5 até casar. VULNERÁVEL = achou `123456`. Rápido (10^6 hashes).
- **#14**: grava um arquivo com `CifraArquivo.cifrar("operadorX")` e decifra com
  `CifraArquivo.decifrar` (a chave está no código, ataque #14). VULNERÁVEL = recuperou
  `operadorX`.

Hoje v1 e v2 são o mesmo código, então todos dão `VULNERAVEL` nos dois lados; as proteções do
v2 (fase 3) viram cada um para `PROTEGIDO` no lado protegido, com a expectativa atualizada.

## 5. Saídas e critério de pronto

- `regras/ataques-exemplo.json` com as regras dos ataques por regra, carregável na tela e pelo
  harness.
- `AtaquesIT` roda os 14 ataques × 2 lados sob `-Pintegracao`, comparando com
  `expectativas-ataques.properties` e falhando se divergir.
- A matriz HTML é gerada em `resultados/` ao final da execução.
- `GeradorMatrizAtaques` coberto por teste de unidade (o HTML contém as células e os vereditos
  certos).
- `mvn -B clean verify -Pintegracao` verde no reator inteiro.

O benchmark comparativo e as proteções do v2 ficam para a fase 3.
