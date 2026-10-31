package br.com.ccbank.servidor.relatorio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Uma linha do jornal como aparece no relatório; cooperativa, PAC e terminal são os alegados na mensagem. */
public final class LinhaRelatorio {

    private final long id;
    private final LocalDateTime dataHora;
    private final String tipo;
    private final String operador;
    private final String cooperativa;
    private final String pac;
    private final String terminal;
    private final BigDecimal valor;
    private final String contaOrigem;
    private final String contaDestino;
    private final String codigoRetorno;
    private final boolean intercredis;
    private final boolean divergente;
    private final String terminalCadastrado;

    public LinhaRelatorio(long id, LocalDateTime dataHora, String tipo, String operador,
            String cooperativa, String pac, String terminal, BigDecimal valor,
            String contaOrigem, String contaDestino, String codigoRetorno,
            boolean intercredis, boolean divergente, String terminalCadastrado) {
        this.id = id;
        this.dataHora = dataHora;
        this.tipo = tipo;
        this.operador = operador;
        this.cooperativa = cooperativa;
        this.pac = pac;
        this.terminal = terminal;
        this.valor = valor;
        this.contaOrigem = contaOrigem;
        this.contaDestino = contaDestino;
        this.codigoRetorno = codigoRetorno;
        this.intercredis = intercredis;
        this.divergente = divergente;
        this.terminalCadastrado = terminalCadastrado;
    }

    public long id() {
        return id;
    }

    public LocalDateTime dataHora() {
        return dataHora;
    }

    public String tipo() {
        return tipo;
    }

    public String operador() {
        return operador;
    }

    public String cooperativa() {
        return cooperativa;
    }

    public String pac() {
        return pac;
    }

    public String terminal() {
        return terminal;
    }

    /** Valor da operação; {@code null} quando não há (login, saldo, extrato, valor inválido). */
    public BigDecimal valor() {
        return valor;
    }

    /** {@code cccc/nnnnnn}, ou vazio. */
    public String contaOrigem() {
        return contaOrigem;
    }

    /** {@code cccc/nnnnnn}, ou vazio. */
    public String contaDestino() {
        return contaDestino;
    }

    public String codigoRetorno() {
        return codigoRetorno;
    }

    public boolean intercredis() {
        return intercredis;
    }

    public boolean divergente() {
        return divergente;
    }

    /** {@code cccc/ppp/ttt} do operador, {@code operador inexistente}, ou vazio em logins. */
    public String terminalCadastrado() {
        return terminalCadastrado;
    }
}
