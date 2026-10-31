package br.com.ccbank.servidor.dados;

import java.math.BigDecimal;

/**
 * Uma linha da tabela {@code transacao}, preenchida aos poucos durante o processamento.
 * Campos vindos da mensagem são guardados como texto bruto, exatamente como chegaram.
 */
public final class RegistroJornal {

    private String tipo;
    private String operadorLogin;
    private String cooperativa;
    private String pac;
    private String terminal;
    private Integer contaOrigemId;
    private String cooperativaOrigem;
    private String contaOrigem;
    private Integer contaDestinoId;
    private String cooperativaDestino;
    private String contaDestino;
    private BigDecimal valor;
    private BigDecimal saldoAposOrigem;
    private BigDecimal saldoAposDestino;
    private String codigoRetorno;
    private boolean intercredis;
    private String ipOrigem;
    private Integer duracaoMs;

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getOperadorLogin() {
        return operadorLogin;
    }

    public void setOperadorLogin(String operadorLogin) {
        this.operadorLogin = operadorLogin;
    }

    public String getCooperativa() {
        return cooperativa;
    }

    public void setCooperativa(String cooperativa) {
        this.cooperativa = cooperativa;
    }

    public String getPac() {
        return pac;
    }

    public void setPac(String pac) {
        this.pac = pac;
    }

    public String getTerminal() {
        return terminal;
    }

    public void setTerminal(String terminal) {
        this.terminal = terminal;
    }

    public Integer getContaOrigemId() {
        return contaOrigemId;
    }

    public void setContaOrigemId(Integer contaOrigemId) {
        this.contaOrigemId = contaOrigemId;
    }

    public String getCooperativaOrigem() {
        return cooperativaOrigem;
    }

    public void setCooperativaOrigem(String cooperativaOrigem) {
        this.cooperativaOrigem = cooperativaOrigem;
    }

    public String getContaOrigem() {
        return contaOrigem;
    }

    public void setContaOrigem(String contaOrigem) {
        this.contaOrigem = contaOrigem;
    }

    public Integer getContaDestinoId() {
        return contaDestinoId;
    }

    public void setContaDestinoId(Integer contaDestinoId) {
        this.contaDestinoId = contaDestinoId;
    }

    public String getCooperativaDestino() {
        return cooperativaDestino;
    }

    public void setCooperativaDestino(String cooperativaDestino) {
        this.cooperativaDestino = cooperativaDestino;
    }

    public String getContaDestino() {
        return contaDestino;
    }

    public void setContaDestino(String contaDestino) {
        this.contaDestino = contaDestino;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public BigDecimal getSaldoAposOrigem() {
        return saldoAposOrigem;
    }

    public void setSaldoAposOrigem(BigDecimal saldoAposOrigem) {
        this.saldoAposOrigem = saldoAposOrigem;
    }

    public BigDecimal getSaldoAposDestino() {
        return saldoAposDestino;
    }

    public void setSaldoAposDestino(BigDecimal saldoAposDestino) {
        this.saldoAposDestino = saldoAposDestino;
    }

    public String getCodigoRetorno() {
        return codigoRetorno;
    }

    public void setCodigoRetorno(String codigoRetorno) {
        this.codigoRetorno = codigoRetorno;
    }

    public boolean isIntercredis() {
        return intercredis;
    }

    public void setIntercredis(boolean intercredis) {
        this.intercredis = intercredis;
    }

    public String getIpOrigem() {
        return ipOrigem;
    }

    public void setIpOrigem(String ipOrigem) {
        this.ipOrigem = ipOrigem;
    }

    public Integer getDuracaoMs() {
        return duracaoMs;
    }

    public void setDuracaoMs(Integer duracaoMs) {
        this.duracaoMs = duracaoMs;
    }
}
