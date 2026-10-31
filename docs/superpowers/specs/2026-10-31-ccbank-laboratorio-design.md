# CC_Bank — Laboratório de segurança de transações bancárias

**Data:** 2026-10-31
**Status:** aprovada. Revisões em 2026-10-31: (1) validação da senha do cliente em transação separada (seções 4.4, 4.5, 7.3, 8.1, 9.2); (2) senhas trafegadas e armazenadas em MD5 e último login cifrado com AES por lado (seções 4.2, 4.5, 5.1, 6, 7.3, 8.1).

## 1. Objetivo

Construir um sistema bancário cooperativo simples e **propositalmente sem segurança** (lado esquerdo, v1), um interceptador capaz de adulterar as mensagens trocadas, e um segundo ambiente (lado direito, v2) que começa idêntico ao v1 e recebe, ao longo do projeto, medidas de segurança. O sucesso é medido por:

- conseguir adulterar transações no v1 (ataques reproduzíveis);
- verificar que cada evolução do v2 barra os ataques correspondentes;
- quantificar o impacto de performance de cada evolução (v1 vs v2, e camada a camada).

Nesta primeira etapa **não há** criptografia, token de sessão, hash de senha ou qualquer outra proteção.

## 2. Restrições técnicas

- Bytecode Java 8: compilado com o JDK 11 instalado, usando `--release 8` (Maven 3.9).
- Interface gráfica em Swing.
- Comunicação via socket TCP com o protocolo proprietário de rótulo/valor (seção 4).
- PostgreSQL 18 instalado localmente. Credenciais em `config/db.properties` (fora do git), com o modelo `config/db.properties.example` versionado. Porta padrão 5432.
- Dependências (todas compatíveis com Java 8): driver PostgreSQL JDBC 42.7.x, HikariCP 4.0.3, SLF4J + Logback 1.3.x, Jackson 2.x, HdrHistogram, JUnit 5. Sem Spring e sem framework de migração.
- Scripts de operação em PowerShell (Windows).

## 3. Arquitetura

### 3.1 Abordagem: núcleo compartilhado + pipeline de camadas

O código comum (protocolo, domínio, telas-base) é compartilhado. Cada lado monta um **pipeline** de etapas por onde a mensagem passa antes de ir ao socket e ao voltar dele:

- v1: `[serializar] → [enviar]`
- v2 (futuro, exemplo): `[serializar] → [assinar] → [cifrar] → [enviar]`

O servidor tem o pipeline simétrico. Regras de negócio que diferem entre versões ficam em classes específicas de cada versão. As camadas do v2 são ligadas e desligadas por configuração, o que permite medir o custo de cada uma isoladamente. Se uma evolução for radical demais para o pipeline (por exemplo, protocolo binário), aquela parte ganha implementação própria no v2.

O **v1 fica congelado** ao fim da fase 1. Recebe apenas correções de bug que não alterem seu comportamento vulnerável.

### 3.2 Módulos (Maven multi-módulo)

```
CC_Bank/
├── ccbank-protocol/     Mensagem, codec texto, catálogo de rótulos, interfaces do pipeline, formatador de log
├── ccbank-server/       Servidor TCP, regras de negócio, acesso ao banco, ferramenta administrativa (relatórios)
├── ccbank-client/       Cliente Swing com as duas metades
├── ccbank-interceptor/  Proxy TCP com tela Swing (passagem, manual, regras) e modo headless
├── ccbank-tests/        Testes de integração, de ataque, gerador de carga, relatório comparativo
├── db/v1/, db/v2/       Scripts SQL numerados (schema + seed + massa de carga + reset)
├── config/              Arquivos .properties
├── regras/              Arquivos de regras do interceptador
├── scripts/             PowerShell: setup-db, start-all, stop-all, benchmark
├── logs/                Gerado em execução (fora do git)
├── relatorios/          Relatórios administrativos gerados (fora do git)
└── resultados/          Resultados de benchmark; só historico.csv é versionado
```

### 3.3 Topologia em execução

| Componente | Porta | Destino |
|---|---|---|
| Servidor v1 (`--env=v1`) | 9001 | banco `ccbank_v1` |
| Servidor v2 (`--env=v2`) | 9002 | banco `ccbank_v2` |
| Interceptador, aba Esquerdo | 8001 | 127.0.0.1:9001 |
| Interceptador, aba Direito | 8002 | 127.0.0.1:9002 |

- O servidor é um único código iniciado em **dois processos independentes**. O argumento `--env` escolhe porta, banco, pipeline e regras de negócio.
- Cada metade do cliente mantém **uma conexão TCP persistente** com requisição/resposta síncrona (uma mensagem em voo por vez).
- O servidor usa um pool de threads, com uma thread por conexão.
- Saque, depósito e transferência executam em transação de banco com `SELECT ... FOR UPDATE`. Em transferências, as contas são travadas sempre em ordem crescente de id para evitar deadlock.
- O liga/desliga do interceptador no cliente apenas troca a porta de destino (9001 ↔ 8001, 9002 ↔ 8002) e reconecta.

## 4. Protocolo

### 4.1 Formato

- Texto UTF-8.
- Um campo por linha: `RRRRR=valor`, sendo `RRRRR` um rótulo de 5 dígitos. A linha é quebrada no **primeiro** `=`, então o valor pode conter `=`, mas não pode conter quebra de linha.
- Linhas terminadas por `\n`. Uma **linha vazia** encerra a mensagem.
- A mensagem é uma **lista ordenada de campos**. Rótulos podem se repetir.
- Rótulos desconhecidos são ignorados pelo servidor.
- Limite de 64 KB por mensagem, apenas para robustez. Mensagens acima disso são descartadas e a conexão é encerrada.
- Representação: valor monetário com ponto e duas casas (`150.00`); data `AAAA-MM-DD`; data-hora `AAAA-MM-DDTHH:MM:SS`.

### 4.2 Catálogo de rótulos

| Rótulo | Nome no log | Significado |
|---|---|---|
| 00001 | transacao | `login`, `validar_senha`, `saldo`, `saque`, `deposito`, `transferencia`, `extrato` |
| 00002 | operador | login do operador |
| 00003 | senha_operador | MD5 da senha do operador (somente no login) |
| 00004 | cooperativa_operador | cooperativa do operador |
| 00005 | pac | PAC do operador |
| 00006 | terminal | terminal do operador |
| 00007 | nome_operador | nome do operador (resposta do login) |
| 00010 | cooperativa_conta | cooperativa da conta |
| 00011 | conta | número da conta |
| 00012 | senha_conta | MD5 da senha da conta (somente na `validar_senha`) |
| 00020 | cooperativa_destino | cooperativa da conta destino |
| 00021 | conta_destino | número da conta destino |
| 00030 | valor | valor da operação |
| 00031 | data_inicio | início do período do extrato |
| 00032 | data_fim | fim do período do extrato |
| 00080 | qtd_lancamentos | quantidade de lançamentos no extrato |
| 00081 | lanc_data_hora | data-hora do lançamento (um grupo por lançamento) |
| 00082 | lanc_tipo | descrição/tipo do lançamento |
| 00083 | lanc_valor | valor com sinal |
| 00084 | lanc_saldo_apos | saldo após o lançamento |
| 00085 | lanc_contraparte | conta contraparte (transferências) |
| 00090 | codigo_retorno | código de retorno |
| 00091 | mensagem_retorno | mensagem de retorno em texto |
| 00092 | saldo | saldo atual |
| 00093 | titular | nome do titular |
| 00094 | id_transacao | id da transação no jornal |

O catálogo vive no módulo `ccbank-protocol` e é a fonte única dos nomes usados em logs, no interceptador e no painel de mensagens do cliente.

### 4.3 Códigos de retorno (00090)

| Código | Significado |
|---|---|
| 000 | sucesso |
| 001 | operador ou senha inválidos |
| 002 | senha da conta inválida |
| 003 | saldo insuficiente |
| 004 | conta inexistente |
| 005 | valor inválido |
| 006 | mensagem malformada ou campo obrigatório ausente |
| 007 | conta destino igual à conta de origem |
| 099 | erro interno |

### 4.4 Campos por transação

| Transação | Requisição (obrigatórios) | Resposta (além de 00090/00091) |
|---|---|---|
| login | 00002, 00003 | 00004, 00005, 00006, 00007 |
| validar_senha | 00002, 00004–00006, 00010, 00011, 00012 | 00093 (em caso de sucesso), 00094 |
| saldo | 00002, 00004–00006, 00010, 00011 | 00092, 00093, 00094 |
| saque | idem saldo + 00030 | 00092, 00093, 00094 |
| deposito | idem saldo + 00030 | 00092, 00093, 00094 |
| transferencia | idem saldo + 00020, 00021, 00030 | 00092 (saldo da origem), 00093, 00094 |
| extrato | idem saldo + 00031, 00032 (opcionais) | 00093, 00094, 00080, grupos 00081–00085 |

### 4.5 Regras de negócio (v1)

- O login apenas valida operador e senha e devolve cooperativa/PAC/terminal cadastrados. **Não há sessão**: toda mensagem seguinte traz 00002 e 00004–00006, e o servidor v1 **confia** nesses campos sem conferir.
- A senha do cliente é conferida numa **transação separada**, `validar_senha`, antes de **todas** as operações (saldo, saque, depósito, transferência e extrato). Na transferência, valida-se a conta de origem. A validação responde `000`, `002` (senha inválida), `004` (conta inexistente) ou `006`; não altera saldo nem gera lançamento, mas é gravada no jornal.
- As operações não levam a senha (00012); se vier, é ignorada. O cliente só envia a operação depois de receber `000` na validação.
- As senhas (00003 e 00012) trafegam como **MD5 em hexadecimal minúsculo** (32 caracteres, bytes UTF-8, sem salt), calculado pelo cliente. O banco guarda o mesmo MD5 e o servidor compara o texto recebido com o guardado.
- **O servidor v1 não liga a validação à operação**: aceita a operação sem verificar se houve validação antes (não há sessão). Essa é uma vulnerabilidade de propósito do v1 (ataques #10 e #11 da seção 7.3).
- Saque e transferência são recusados se o saldo for insuficiente. Não há cheque especial.
- Valor deve ser maior que zero e ter no máximo duas casas decimais.
- O operador pode atender contas de qualquer cooperativa. Uma operação é **Intercredis** quando a cooperativa da conta (de origem, na transferência) é diferente da cooperativa do operador informada na mensagem. Intercredis não tem regra própria; é apenas marcada no jornal.
- Transferência entre cooperativas diferentes é permitida.
- Extrato: período informado ou, na falta dele, os últimos 30 dias; no máximo 200 lançamentos, os mais recentes.

## 5. Modelo de dados

Dois bancos, `ccbank_v1` e `ccbank_v2`, para uso manual; e `ccbank_v1_teste` e `ccbank_v2_teste`, para testes automatizados e benchmarks. Os scripts de `db/v2/` começam como cópia exata dos de `db/v1/`. Fuso horário: America/Sao_Paulo.

### 5.1 Tabelas

- **cooperativa**: id, codigo `CHAR(4)` único, nome.
- **pac**: id, cooperativa_id, codigo `CHAR(3)`, nome. Único por (cooperativa_id, codigo).
- **terminal**: id, pac_id, codigo `CHAR(3)`. Único por (pac_id, codigo).
- **operador**: id, login único, senha `CHAR(32)` (MD5 em hexadecimal, sem salt), nome, terminal_id **único** (um operador por terminal), ativo.
- **cliente**: id, nome, cpf único.
- **conta**: id, cooperativa_id, numero `CHAR(6)`, cliente_id único (uma conta por titular), senha `CHAR(32)` (MD5 em hexadecimal, sem salt), saldo `NUMERIC(15,2)` com `CHECK (saldo >= 0)`. Única por (cooperativa_id, numero).
- **transacao** (jornal):
  - id, data_hora, tipo (`LOGIN`, `VALIDAR_SENHA`, `SALDO`, `SAQUE`, `DEPOSITO`, `TRANSFERENCIA`, `EXTRATO`, `INVALIDA`);
  - operador_login, cooperativa, pac, terminal — **texto, exatamente como vieram na mensagem**;
  - conta_origem_id e conta_destino_id (referências opcionais), mais cooperativa/número de origem e destino em texto;
  - valor, saldo_apos_origem, saldo_apos_destino;
  - codigo_retorno, intercredis (boolean), ip_origem, duracao_ms.
  - **Todas as tentativas são gravadas**, inclusive as recusadas e os logins.
- **lancamento** (extrato): id, conta_id, transacao_id, data_hora, natureza (`C`/`D`), descricao, valor, saldo_apos. A transferência gera dois lançamentos. Saldo, extrato e operações recusadas não geram lançamento.

Índices: `transacao(data_hora)`, `lancamento(conta_id, data_hora)`.

### 5.2 Dados iniciais (iguais nos dois lados)

- 3 cooperativas (0001, 0002, 0003), 2 PACs em cada, 2 terminais por PAC: 12 terminais.
- 12 operadores, um por terminal, incluindo `rodrigop`. Senhas simples, documentadas no README, gravadas com `md5()` do PostgreSQL.
- 30 contas, 10 por cooperativa, com saldos entre 0 e 10.000 e senhas de 6 dígitos documentadas.
- Script de **massa de carga**, que cria milhares de contas para os benchmarks.
- Script de **reset**, que limpa jornal e lançamentos e restaura os saldos ao estado inicial.

## 6. Logs

- SLF4J + Logback 1.3.x, rotação diária, retenção de 30 dias.
- Arquivos em `logs/`: `client-esquerdo.log`, `client-direito.log`, `server-v1.log`, `server-v2.log`, `interceptor-esquerdo.log`, `interceptor-direito.log`, `admin.log`.
- Cada mensagem é registrada com cabeçalho (data-hora, lado, direção, endereço, quantidade de campos, tempo de resposta quando aplicável) e um campo por linha: rótulo, nome legível e valor. O código de retorno também mostra seu significado. O protocolo não é alterado.

```
2026-10-31 14:03:22.118 [ESQ] ENVIADA → 127.0.0.1:9001 (9 campos)
    00001 transacao ........... saque
    00002 operador ............ rodrigop
    00030 valor ............... 150.00
2026-10-31 14:03:22.164 [ESQ] RECEBIDA ← 127.0.0.1:9001 (5 campos) em 46 ms
    00090 codigo_retorno ...... 000 (sucesso)
```

- O servidor registra também tempo de processamento, resultado e id do jornal de cada requisição.
- O interceptador registra as quatro etapas de cada troca (recebida do cliente, enviada ao servidor, recebida do servidor, devolvida ao cliente) e marca os campos adulterados com valor anterior, valor novo e origem da alteração:

```
    00030 valor ............... 150.00  →  1.50   [ADULTERADO: regra "saque-centavos"]
```

- Senhas aparecem como MD5 sem salt nos logs do v1 (equivalentes à senha para autenticar), de propósito.
- Nos benchmarks, o nível de log é configurável e sempre **igual nos dois lados**. O padrão é um resumo por transação.

## 7. Interceptador

- Aplicativo Swing com duas abas, **Esquerdo** (8001 → 9001) e **Direito** (8002 → 9002).
- O núcleo (proxy + regras) é independente da tela e roda em modo headless: `--headless --regras=<arquivo.json>`.
- Interpreta as mensagens com o codec do protocolo. Se uma mensagem não puder ser interpretada (por exemplo, futura cifragem no v2), entra em **modo opaco**: repassa os bytes intactos e registra o conteúdo em hexadecimal.

### 7.1 Modos (por aba)

- **Passagem**: apenas observa e registra.
- **Manual**: cada mensagem fica retida numa tabela editável (rótulo | nome | valor). É possível alterar valores, adicionar e remover campos, e então **Encaminhar**, **Encaminhar original** ou **Descartar**. Um filtro define o que é retido: requisições, respostas ou ambas.
- **Regras**: aplica automaticamente as regras ativas do arquivo carregado.

Cada aba mantém um **histórico** de trocas (hora, direção, transação, se foi adulterada) com detalhe antes/depois, e um botão **Reenviar** que repete uma requisição capturada N vezes ao servidor (replay).

### 7.2 Formato das regras (JSON)

```json
{
  "nome": "saque-centavos",
  "ativa": true,
  "direcao": "REQUISICAO",
  "quando": { "00001": "saque" },
  "acoes": [ { "tipo": "SET", "rotulo": "00030", "valor": "0.01" } ]
}
```

- `direcao`: `REQUISICAO` ou `RESPOSTA`.
- `quando`: todos os rótulos listados devem casar. Valor literal compara por igualdade; valor com prefixo `~` é expressão regular.
- `acoes`: `SET` (altera o valor do rótulo; se repetido, todas as ocorrências), `ADD` (acrescenta um campo ao final), `REMOVE` (remove todas as ocorrências do rótulo).
- Arquivo é uma lista de regras, aplicadas na ordem.

### 7.3 Catálogo de ataques (`regras/ataques-exemplo.json`)

| # | Ataque | Direção | Efeito esperado no v1 |
|---|---|---|---|
| 1 | Valor adulterado | requisição | saque de 500.00 chega como 5.00 |
| 2 | Depósito inflado | requisição | depósito de 10.00 chega como 10000.00 |
| 3 | Transferência desviada | requisição | conta destino trocada |
| 4 | Origem falsificada | requisição | cooperativa/PAC/terminal trocados; aparece no relatório de divergência |
| 5 | Operador falsificado | requisição | operação registrada em nome de outro operador |
| 6 | Resposta falsa | resposta | 00090=003 vira 000 e o saldo é inventado |
| 7 | Saldo exibido falso | resposta | 00092 alterado na resposta de saldo |
| 8 | Replay | — | saque/depósito capturado reenviado N vezes e efetivado N vezes |
| 9 | Captura passiva | — | senhas de operador e de conta legíveis no log do interceptador |
| 10 | Validação falsificada | resposta | `validar_senha` com senha errada: 00090=002 vira 000 e o cliente segue para a operação |
| 11 | Operação sem validação | — | saque/transferência enviado (ou reenviado) sem `validar_senha` antes é efetivado |
| 12 | Reuso do hash (*pass-the-hash*) | requisição | MD5 capturado no log/interceptador é aceito como se fosse a senha |
| 13 | Quebra do hash | — | MD5 sem salt de senha de 6 dígitos (ou senha fraca de operador) é quebrado por força bruta |
| 14 | Chave AES no código | — | chave extraída do `.jar` decifra `dados/ultimo-login-*.dat` e revela o último operador de cada lado |

### 7.4 Comportamento do cliente com o interceptador

- Timeout de leitura do cliente configurável, padrão **120 s**, para suportar o modo manual.
- Se o interceptador estiver ligado no cliente mas não estiver no ar, o cliente exibe "Interceptador não disponível em 127.0.0.1:8001" (ou 8002).

## 8. Telas

### 8.1 Cliente

- Janela 1280×800 dividida em **duas metades iguais e fixas**: Esquerdo · v1 original | Direito · v2 evoluções. Cada metade é o mesmo componente (`PainelLado`), configurado por versão.
- Cabeçalho de cada metade: estado da conexão (host:porta), caixa "Via interceptador" (alterável apenas com a metade deslogada) e dados do operador logado (login, cooperativa, PAC, terminal).
- Fluxo: tela de login do operador → barra de operações **Saldo, Saque, Depósito, Transferência, Extrato, Sair**. Sair apenas volta ao login.
- O cliente envia o MD5 das senhas digitadas (operador e cliente); os campos continuam recebendo a senha normal.
- **Último login por lado:** após um login bem-sucedido, o login é gravado em `dados/ultimo-login-esquerdo.dat` ou `dados/ultimo-login-direito.dat`, cifrado com `AES/CBC/PKCS5Padding` (chave de 128 bits fixa no código, IV aleatório no início do arquivo, conteúdo em Base64). Ao abrir, cada metade preenche o campo de usuário com o seu último login (nunca a senha); arquivo ausente ou inválido deixa o campo vazio e registra um aviso no log.
- Formulários com cooperativa e número da conta, valor quando aplicável, conta destino na transferência, período no extrato e **senha do cliente** em campo mascarado.
- Executar faz duas chamadas na mesma tarefa: primeiro `validar_senha`; só com `000` envia a operação, sem a senha. Falha na validação mostra o erro e não envia a operação. O painel de mensagens mostra as duas trocas e o resultado mostra o tempo de cada etapa.
- Resultado: mensagem de retorno, titular, saldo, id da transação e tempo de resposta em ms. O extrato aparece em tabela (data/hora, descrição, C/D, valor, saldo após).
- Painel recolhível **Mensagens** em cada metade, com as mensagens enviadas e recebidas usando os nomes legíveis.
- Chamadas de rede em `SwingWorker`. Erros de conexão aparecem na própria metade.

### 8.2 Ferramenta administrativa (relatórios)

- Janela Swing iniciada à parte, com classe principal no módulo `ccbank-server`. Lê o banco diretamente.
- Entradas: ambiente (v1 ou v2); período (hoje, uma data ou intervalo); filtros de cooperativa, PAC, terminal, operador e tipo de operação; caixas "só Intercredis", "só divergências de origem" e "incluir recusadas".
- **Gerar** cria `relatorios/relatorio-<env>-<AAAA-MM-DD-HHMMSS>.html` autocontido (CSS embutido, sem dependências externas) e o abre no navegador padrão.
- Conteúdo do HTML:
  - resumo: quantidade e soma por tipo de operação, total de recusadas, de Intercredis e de divergências;
  - tabela: data/hora, cooperativa, PAC, terminal (alegados), operador, operação, valor, conta origem, conta destino, retorno, Intercredis, divergência.
- **Divergência de origem**: a linha é marcada quando cooperativa/PAC/terminal alegados na mensagem não correspondem ao terminal cadastrado do operador (ou quando o operador não existe). Linhas divergentes ficam destacadas, com o terminal cadastrado exibido para comparação. No v1 isso não bloqueia nada; apenas evidencia.

## 9. Testes e benchmark

### 9.1 Testes de unidade (`mvn test`, sem banco)

- Codec: ida e volta, rótulos repetidos, `=` no valor, mensagem malformada, limite de 64 KB.
- Motor de regras do interceptador: condições literais e regex, `SET`, `ADD`, `REMOVE`, direção.
- Formatador de log com nomes legíveis.
- Validação de valor monetário.

### 9.2 Testes de integração (`mvn verify -Pintegracao`, PostgreSQL real)

A mesma suíte roda contra v1 e v2 (teste parametrizado JUnit 5), com servidor em memória sobre `ccbank_v1_teste` / `ccbank_v2_teste`:

- login válido e inválido;
- cada operação com sucesso, verificando saldo, jornal e lançamentos;
- `validar_senha` com cada código de retorno, gravada no jornal; operação enviada sem validação prévia é aceita pelo v1 (documenta a vulnerabilidade);
- saldo insuficiente, senha errada, conta inexistente, valor inválido, transferência para a própria conta;
- marcação Intercredis; conteúdo e limites do extrato; gravação de recusas no jornal; detecção de divergência pela consulta do relatório;
- concorrência: 100 saques simultâneos na mesma conta; o saldo nunca fica negativo e soma de saldos, jornal e lançamentos fecham.

### 9.3 Testes de ataque (mesmo perfil `-Pintegracao`)

- Cada ataque do catálogo roda pelo interceptador headless contra os dois lados.
- O resultado (VULNERÁVEL ou PROTEGIDO) é determinado pela resposta e pelo estado do banco.
- `expectativas-ataques.properties` declara o resultado esperado por ataque e lado. Inicialmente todos VULNERÁVEL nos dois lados; o v2 é atualizado conforme as evoluções entram. O teste falha se o resultado divergir da expectativa.
- Gera uma **matriz de ataques** em HTML (ataque × lado).

### 9.4 Benchmark comparativo (`scripts/benchmark.ps1 -Perfil <smoke|medio|pesado>`)

- Preparação: reset dos dois bancos de teste com massa de carga; os dois servidores sobem como processos separados com a mesma configuração de JVM.
- Execução: v1 primeiro, v2 depois, **nunca simultâneos**; fase de aquecimento descartada; **3 repetições**, usando a mediana.
- Cada cliente virtual faz login e executa o mix de operações (padrão: 40% saldo, 20% saque, 20% depósito, 15% transferência, 5% extrato) sobre contas sorteadas com **semente fixa**, garantindo a mesma sequência nos dois lados.

| Perfil | Clientes simultâneos | Transações por rodada |
|---|---|---|
| smoke | 5 | 1.000 |
| medio (padrão) | 50 | 50.000 |
| pesado | 200 | 500.000 |

- Métricas, gerais e por tipo de operação: transações por segundo; latência p50/p95/p99/máx (HdrHistogram); taxa de erro; tamanho médio das mensagens em bytes; verificação de consistência ao final.
- Cenários de camadas: o benchmark aceita configurações do pipeline v2 (por exemplo `v2[assinatura]`, `v2[assinatura+cifra]`) para medir cada camada isoladamente.
- Saída:
  - `resultados/<AAAA-MM-DD-HHMMSS>/relatorio.html` com v1 vs v2 lado a lado e o percentual de impacto em cada métrica, mais um gráfico da evolução histórica;
  - uma linha acrescentada a `resultados/historico.csv` com data, commit do git, perfil, configuração do v2 e métricas.

## 10. Fases de implementação

1. **Base (lado esquerdo):** protocolo, logs, banco (scripts, seed, setup), servidor, cliente (metade esquerda funcional; direita como placeholder), ferramenta administrativa, testes de unidade e de integração.
2. **Interceptador:** proxy com os três modos, motor de regras, catálogo de ataques, testes de ataque e matriz.
3. **Lado direito e benchmark:** v2 funcional como cópia do v1 com o pipeline pronto para receber camadas, metade direita do cliente, gerador de carga e relatório comparativo.

Cada fase termina com seus testes passando e com o sistema utilizável até aquele ponto.

## 11. Fora de escopo nesta etapa

- Qualquer medida de segurança (criptografia, assinatura, sessão, hash de senha, mascaramento de log). Essas são as evoluções futuras do v2, cada uma com sua própria spec.
- Relatórios dentro do cliente Swing.
- Cheque especial, tarifas e regras específicas de Intercredis.
- Mais de uma conta por titular.
