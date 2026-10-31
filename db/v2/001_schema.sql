-- CC_Bank: schema do banco (spec, seção 5).
-- Senhas em texto puro e campos recebidos gravados como texto bruto: inseguro de propósito no v1.

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
