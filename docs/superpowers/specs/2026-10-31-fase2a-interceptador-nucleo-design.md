# Fase 2a — Núcleo do interceptador (headless)

**Data:** 2026-10-31
**Status:** rascunho para revisão.
**Spec-mãe (fonte de verdade):** `docs/superpowers/specs/2026-10-31-ccbank-laboratorio-design.md`, seções 3.3, 4, 6, 7 e 9.

## 1. Objetivo e escopo

Entregar o **núcleo do interceptador** do laboratório: um proxy TCP que fica entre o
cliente e o servidor, interpreta as mensagens com o codec do protocolo e, opcionalmente,
as adultera segundo um arquivo de regras. Tudo operável **sem tela**, por linha de comando
(`--headless`), para permitir rodar ataques de ponta a ponta já nesta fase.

**Dentro do escopo (2a):**

- Motor de regras (função pura sobre `Mensagem`): condições literais e regex, ações
  `SET`/`ADD`/`REMOVE`, filtro por sentido, ordem das regras.
- Carregamento das regras de um arquivo JSON (formato da seção 7.2 da spec-mãe).
- Proxy TCP por lado: loop de troca, modo passagem e modo regras, modo opaco e limite de
  64 KB.
- CLI headless (`--headless --lado=... [--regras=...]`).
- Logs do interceptador por lado, com as quatro etapas de cada troca e marcação dos campos
  adulterados.
- Testes de unidade (motor e carregamento) e de integração leve (proxy sobre sockets
  locais, sem banco).

**Fora do escopo (fases seguintes):**

- Tela Swing, modo manual, histórico e replay → **2b**.
- Catálogo de ataques, testes de ataque parametrizados por lado, matriz HTML → **2c**.
- Qualquer medida de segurança (isso é v2).

## 2. Restrições

Herdadas da spec-mãe, seção 2: bytecode Java 8 (`--release 8` no JDK 11), dependências
compatíveis com Java 8 e versões centralizadas no `dependencyManagement` da raiz, UTF-8 em
tudo, scripts em PowerShell. Nenhuma dependência nova: o módulo usa `ccbank-protocol`,
Jackson (`jackson-databind` 2.17.2, já gerenciado), SLF4J + Logback e JUnit 5.

O limite de **64 KB por mensagem** (robustez, não segurança) vale aqui como nos dois lados.

## 3. Módulo e estrutura

Novo módulo Maven `ccbank-interceptor`, acrescentado ao `pom.xml` raiz, pacote base
`br.com.ccbank.interceptador`.

```
ccbank-interceptor/
 └ src/main/java/br/com/ccbank/interceptador/
    regras/
      Sentido               enum REQUISICAO | RESPOSTA
      Condicao              rótulos → padrão (literal ou ~regex); todos devem casar
      Acao                  tipo (SET|ADD|REMOVE), rótulo, valor
      Regra                 nome, ativa, sentido, condição, lista de ações
      Adulteracao           rótulo, antes, depois, nome da regra (para o log)
      ResultadoAplicacao    Mensagem resultante + lista de Adulteracao
      MotorRegras           aplicar(Mensagem, Sentido) → ResultadoAplicacao
      CarregadorRegras      lê o JSON (lista de regras) → List<Regra>
    proxy/
      ParDeConexoes         o loop de troca de um par cliente↔servidor
      ServidorProxy         escuta a porta do lado, aceita, pool de threads
    config/
      LadoInterceptador     enum ESQUERDO (8001→9001) | DIREITO (8002→9002)
      ConfiguracaoInterceptador  lado, arquivo de regras opcional
    InterceptadorMain       CLI headless
```

O `Sentido` do interceptador (REQUISICAO/RESPOSTA) é distinto do `Direcao`
(ENVIADA/RECEBIDA) já existente em `ccbank-protocol`, que é usado só na formatação de log.

## 4. Motor de regras

Formato do arquivo (seção 7.2 da spec-mãe), lista de regras aplicadas na ordem:

```json
{
  "nome": "saque-centavos",
  "ativa": true,
  "direcao": "REQUISICAO",
  "quando": { "00001": "saque" },
  "acoes": [ { "tipo": "SET", "rotulo": "00030", "valor": "0.01" } ]
}
```

- **`quando`**: todos os rótulos listados têm de casar. Valor literal compara por igualdade;
  valor com prefixo `~` é interpretado como expressão regular sobre o valor do campo.
- **`acoes`**, aplicadas na ordem dentro da regra:
  - `SET`: altera o valor de **todas** as ocorrências do rótulo. **Rótulo ausente: no-op**
    (criar campo é só com `ADD`).
  - `ADD`: acrescenta um novo campo ao final, sempre.
  - `REMOVE`: remove **todas** as ocorrências do rótulo.
- **`direcao`** (`REQUISICAO`/`RESPOSTA`): a chave JSON `direcao` mapeia para o enum `Sentido`;
  a regra só é considerada no sentido correspondente.
- **`ativa`**: quando ausente no JSON, assume `true` (uma regra presente no arquivo vale,
  salvo se desligada explicitamente com `"ativa": false`).
- `MotorRegras.aplicar(Mensagem, Sentido)` percorre, na ordem, apenas as regras `ativa` do
  sentido dado; devolve a `Mensagem` resultante e a lista de `Adulteracao` (rótulo, valor
  anterior, valor novo, nome da regra) para o log. Regra inativa ou de outro sentido é
  ignorada. Sem regras aplicáveis, o resultado é a mensagem intacta e lista vazia.
- O motor **não** toca o protocolo no fio: só reescreve a `Mensagem`, que o proxy
  re-serializa com `CodecMensagem`.

## 5. Proxy

O protocolo é síncrono (uma mensagem em voo por conexão), então o loop por par de conexões
é direto e coincide com as quatro etapas de log:

```
aceita conexão do cliente → abre conexão ao destino do lado
repete:
  lê quadro do cliente   → aplica regras (REQUISICAO) → envia ao servidor
  lê quadro do servidor  → aplica regras (RESPOSTA)   → devolve ao cliente
até uma das pontas fechar
```

- `ServidorProxy` escuta a porta do lado, aceita conexões e trata cada par numa thread de um
  pool (uma thread por par), abrindo uma conexão ao destino por par.
- **Modos:** sem arquivo de regras → **passagem** (motor identidade: observa e loga, não
  altera). Com arquivo → **regras**. O modo **manual** é só da tela (2b).
- **Quadro que não decodifica** (`CodecMensagem` falha ao interpretar os bytes já lidos):
  **modo opaco** — repassa os bytes intactos e registra o conteúdo em hexadecimal. No v1,
  texto puro, isso praticamente não ocorre; existe para a cifragem futura do v2.
- **Quadro acima de 64 KB** (`lerQuadro` estoura o limite): o proxy **derruba a conexão**
  daquele par e registra o motivo. Não tenta recuperar tráfego já inválido; o cliente vê a
  conexão cair e reconecta.
- Fechamento de qualquer ponta encerra o par e libera a conexão ao destino.

## 6. CLI headless

```
InterceptadorMain --headless --lado=esquerdo|direito [--regras=<arquivo.json>]
```

- `--lado` escolhe a tupla porta/destino da seção 3.3 da spec-mãe (esquerdo 8001→9001,
  direito 8002→9002).
- Sem `--regras`, sobe em modo passagem; com `--regras`, em modo regras.
- Dois lados simultâneos = **dois processos**, como os servidores v1/v2 (`--env`).
- Falha ao abrir a porta ou ao ler o arquivo de regras: registra o erro e encerra com código
  de saída diferente de zero (padrão da fase 1).

## 7. Logs

- Arquivos `logs/interceptor-esquerdo.log` e `logs/interceptor-direito.log` (spec-mãe,
  seção 6), rotação diária, retenção de 30 dias, selecionados por lado como no cliente.
- Cada troca registra as **quatro etapas**: recebida do cliente, enviada ao servidor,
  recebida do servidor, devolvida ao cliente — usando `FormatadorLog` (rótulo, nome legível,
  valor; código de retorno com seu significado).
- Campo adulterado é marcado com valor anterior, valor novo e origem:

  ```
      00030 valor ............... 150.00  →  1.50   [ADULTERADO: regra "saque-centavos"]
  ```

- Modo opaco registra o quadro em hexadecimal. Senhas aparecem como MD5 sem salt, de
  propósito (captura passiva é um dos ataques da 2c).

## 8. Testes

### 8.1 Unidade (`mvn -pl ccbank-interceptor test`, sem rede, sem banco)

- **Motor:** condição literal casa e não casa; condição regex (`~`) casa e não casa; várias
  condições no `quando` (todas têm de casar); `SET` altera uma e várias ocorrências; `SET`
  em rótulo ausente é no-op; `ADD` acrescenta ao final; `REMOVE` tira todas as ocorrências;
  regras aplicadas na ordem; regra inativa ignorada; regra de sentido oposto ignorada; lista
  de `Adulteracao` reflete exatamente o que mudou.
- **Carregamento:** JSON válido vira a lista de regras na ordem; `ativa` ausente assume
  `true`; JSON inválido falha com erro claro.

### 8.2 Integração leve (sockets locais em **porta efêmera**, sem banco)

Servidor-eco falso + `ServidorProxy` + cliente socket, provando:

- as quatro etapas de uma troca passam intactas em modo passagem (identidade);
- uma regra `SET` em `REQUISICAO` chega adulterada ao servidor-eco;
- uma regra em `RESPOSTA` chega adulterada ao cliente;
- quadro não-decodificável passa em modo opaco (bytes intactos);
- quadro acima de 64 KB derruba o par.

As portas de teste são efêmeras (bind em porta 0) para não colidir com 8001/8002 nem entre
execuções.

### 8.3 Ajuste de teste existente

O `ClienteBancoIT` (módulo `ccbank-tests`) fixa a porta 8001, que a 2a passa a usar de fato.
Trocar por porta efêmera, para o teste não depender de nenhum interceptador no ar.

## 9. Entregáveis e critério de pronto

- Módulo `ccbank-interceptor` compilando no reator (`mvn -B clean verify`).
- Motor de regras e carregamento com testes de unidade verdes.
- Proxy com testes de integração leve verdes.
- CLI headless sobe um lado, em passagem e em regras.
- Logs por lado com as quatro etapas e marcação de adulteração.
- `ClienteBancoIT` sem porta fixa.

A tela (2b) e o catálogo de ataques (2c) entram depois, cada um com sua própria spec.
