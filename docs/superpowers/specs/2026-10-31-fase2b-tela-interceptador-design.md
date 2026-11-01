# Fase 2b — Tela Swing do interceptador

**Data:** 2026-10-31
**Status:** rascunho para revisão.
**Spec-mãe (fonte de verdade):** `docs/superpowers/specs/2026-10-31-ccbank-laboratorio-design.md`, seção 7 (e 8 para o estilo de tela).
**Depende de:** núcleo headless da fase 2a (`ccbank-interceptor`).

## 1. Objetivo e escopo

Pôr a **tela Swing** do interceptador por cima do núcleo headless da 2a: um aplicativo com
duas abas (Esquerdo 8001→9001, Direito 8002→9002), cada uma operável em três modos
(passagem, manual, regras), com histórico das trocas e replay de uma requisição capturada.
É o console interativo de ataque do laboratório: o operador observa o tráfego e o adultera ao
vivo.

**Dentro do escopo (2b):**

- Refactor do proxy da 2a para consultar um **mediador** por mensagem, mantendo o headless
  idêntico.
- Mediadores de passagem, regras e manual; filtro de retenção no manual.
- Histórico de trocas por aba (antes/depois) e replay direto ao servidor.
- Aplicativo Swing com as duas abas, seletor de modo por aba, carregar arquivo de regras,
  tabela editável no modo manual e tabela de histórico com Reenviar.

**Fora do escopo (2c e além):**

- Catálogo de ataques, testes de ataque parametrizados por lado, matriz HTML → **2c**.
- Qualquer medida de segurança (isso é v2).

## 2. Restrições

Herdadas da spec-mãe, seção 2, e do módulo 2a: bytecode Java 8 (`--release 8` no JDK 11),
dependências compatíveis com Java 8 e versões centralizadas no `dependencyManagement` da raiz,
UTF-8 em tudo, scripts em PowerShell. Sem dependências novas. Interface em Swing. O limite de
**65536 bytes** por mensagem vale como nos dois lados. O timeout de leitura de **120 s** do
cliente (já existente) é o que dá tempo ao modo manual.

## 3. Arquitetura — o mediador

Hoje o `ParDeConexoes` (2a) decide "decodifica → aplica regras → repassa" fixo em `repassar`.
A 2b extrai uma abstração que o proxy consulta por mensagem **decodificável**:

```java
interface Mediador {
    DecisaoMediacao intervir(Mensagem mensagem, Sentido sentido);
}
```

- `DecisaoMediacao`: `ENCAMINHAR` (carrega um `ResultadoAplicacao` — a mensagem possivelmente
  editada mais as adulterações) ou `DESCARTAR`.
- Loop do par: lê quadro → se **não decodifica**, modo **opaco** (repassa os bytes crus, sem
  mediar, como na 2a); se decodifica, chama `intervir`. `ENCAMINHAR` escreve os **bytes
  originais** quando não houve adulteração, ou `codificar(mensagem editada)` quando houve.
  `DESCARTAR` **não escreve nada** — a mensagem some e a outra ponta acaba expirando (é o
  efeito pretendido do descarte). O limite de 64 KB continua derrubando o par.
- **Descarte e o round-trip:** descartar uma requisição pula a leitura da resposta do servidor
  (nada foi enviado); descartar uma resposta não escreve ao cliente. Em ambos, o par volta a
  ler a próxima requisição do cliente, que normalmente já está expirando.

### 3.1 Observação transversal

O registro no histórico independe do modo: um `OuvinteTroca` é chamado a cada troca com o
original, o resultado e o sentido. O headless passa um ouvinte no-op; a tela passa um que
alimenta o histórico da aba.

### 3.2 Compatibilidade com a 2a

Para o headless continuar idêntico, o `ServidorProxy` mantém o construtor atual
`ServidorProxy(int portaEscuta, String hostDestino, int portaDestino, MotorRegras motor,
String sufixoLog)` como conveniência, que embrulha o motor num `MediadorRegras` e usa um
ouvinte no-op. Acrescenta-se o construtor que recebe `Mediador` e `OuvinteTroca`. Os testes da
2a permanecem válidos.

## 4. Mediadores

- `MediadorPassagem` — identidade: encaminha, nunca adultera.
- `MediadorRegras` — embrulha o `MotorRegras` da 2a; `intervir` devolve `ENCAMINHAR` com o
  resultado de `motor.aplicar(mensagem, sentido)`.
- `MediadorManual` — retém a mensagem e bloqueia a thread do par até a tela resolver; respeita
  o filtro de retenção.
- `MediadorModo` (usado pela tela) — delega ao mediador do modo atualmente selecionado na aba,
  trocável em runtime; guarda as regras carregadas (para o modo regras) e o controlador manual.

## 5. Modo manual

- `ControladorManual` (classe **sem Swing**, testável): `intervir(mensagem, sentido)` bloqueia
  numa entrega sincronizada até a tela chamar `encaminhar(mensagemEditada)`,
  `encaminharOriginal()` ou `descartar()`. Um **filtro** (`REQUISICOES`, `RESPOSTAS`, `AMBAS`)
  decide o que é retido; o que não é retido passa como passagem (encaminha sem reter).
- Uma retenção por vez por aba: enquanto uma mensagem está retida, a próxima espera (o
  protocolo é síncrono e há uma conexão por metade, então na prática há no máximo uma em voo).
- Na tela: tabela editável (rótulo | nome | valor) com adicionar e remover linha, e os botões
  **Encaminhar**, **Encaminhar original** e **Descartar**. `Encaminhar` usa a mensagem montada
  da tabela; `Encaminhar original` ignora as edições e manda os bytes originais; `Descartar`
  some com a mensagem.

## 6. Histórico e replay

- `Historico` (sem Swing): lista de `TrocaRegistrada` (hora, sentido, nome da transação, se
  adulterada, mensagem original, resultado). A tela embrulha num `TableModel`; o detalhe de uma
  linha mostra antes/depois reusando `FormatadorAdulteracao`.
- `Replay` (sem Swing): `reenviar(byte[] requisicao, String host, int porta, int vezes)` abre
  uma conexão nova ao servidor de destino, envia os bytes `vezes` vezes, lê cada resposta e as
  devolve. É disparo **cru e direto ao servidor** (não passa pelo mediador), independente de
  haver cliente conectado — caracteriza o ataque de replay do v1. Os bytes reenviados são os
  que foram ao servidor (a requisição capturada no histórico). Roda em `SwingWorker`.

## 7. Tela

Pacote `br.com.ccbank.interceptador.tela`, no estilo do cliente (lógica em classes testáveis,
Swing fino):

- `InterceptadorTelaMain` — `main` que monta um `JFrame` com um `JTabbedPane` de duas
  `PainelAba` (esquerdo e direito).
- `PainelAba` — uma aba: seletor de modo (passagem/manual/regras), estado da conexão
  (porta de escuta → destino), botão **carregar regras** (ativo no modo regras), o painel do
  modo manual (tabela editável + filtro + os três botões) e a tabela de **histórico** com o
  botão **Reenviar** (pede o número de vezes). Cada `PainelAba` hospeda um `ServidorProxy` do
  seu lado, com um `MediadorModo` e um `OuvinteTroca` que alimenta o histórico.
- Carregar um arquivo de regras inválido mostra o erro (`RegrasInvalidasException`) numa caixa
  de diálogo e mantém as regras anteriores.
- Falha ao abrir a porta de escuta (já em uso) aparece na aba, sem derrubar a outra.

## 8. Logs

Reusa os logs por lado da 2a (`logs/interceptor-esquerdo.log`, `interceptor-direito.log`): as
quatro etapas por troca e a marcação de adulteração continuam sendo geradas pelo proxy,
independentemente do modo. Ações da tela (descarte, replay) são registradas no mesmo log do
lado.

## 9. Testes

### 9.1 Unidade (`mvn test`, sem rede, sem banco quando possível)

- `MediadorPassagem`/`MediadorRegras`: `intervir` devolve `ENCAMINHAR` com o resultado certo;
  regras aplicadas no sentido certo.
- `ControladorManual`: `intervir` bloqueia e só retorna quando a tela resolve; `encaminhar`
  devolve a mensagem editada (adulterada), `encaminharOriginal` devolve `ENCAMINHAR` sem
  adulteração, `descartar` devolve `DESCARTAR`; o filtro deixa passar o sentido não retido sem
  bloquear. (Testado com threads.)
- `Historico`: registra cada troca com sentido, transação e marca de adulteração; o detalhe traz
  antes/depois.
- `MediadorModo`: delega ao mediador do modo selecionado e troca em runtime.

### 9.2 Integração leve (sockets locais em porta efêmera, sem banco)

- O proxy com `MediadorManual`: uma requisição retida só chega ao servidor-eco quando a tela
  chama `encaminhar`; `descartar` faz a requisição não chegar; `encaminhar` editado chega
  adulterado.
- `Replay.reenviar(..., n)`: o servidor-eco recebe a requisição `n` vezes e devolve `n`
  respostas.
- Regressão do headless: os testes do `ServidorProxy` da 2a continuam passando (compatibilidade
  do construtor).

### 9.3 Swing

As classes Swing ficam finas; cobre-se o que for lógica pura (ex.: o `TableModel` do histórico
e da tabela editável monta a `Mensagem` a partir das linhas). `java.awt.headless=true` no
surefire, como no cliente.

## 10. Entregáveis e critério de pronto

- `ccbank-interceptor` compilando no reator; headless 2a inalterado e verde.
- Mediadores, `ControladorManual`, `Historico`, `Replay` com testes de unidade/integração leve
  verdes.
- Aplicativo Swing abre com duas abas; cada aba troca de modo, carrega regras, retém/edita/
  encaminha/descarta no manual, mostra o histórico e reenvia uma requisição N vezes.
- Script de operação para abrir a tela do interceptador (em `scripts/`), no estilo dos demais.

O catálogo de ataques, os testes de ataque por lado e a matriz HTML ficam para a 2c.
