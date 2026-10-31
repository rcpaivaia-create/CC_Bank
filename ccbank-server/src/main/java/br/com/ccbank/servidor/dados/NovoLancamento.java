package br.com.ccbank.servidor.dados;

import java.math.BigDecimal;

/** Lançamento a gravar junto com o jornal, quando a transação é efetivada. */
public final class NovoLancamento {

    private final int contaId;
    private final char natureza;
    private final String descricao;
    private final BigDecimal valor;
    private final BigDecimal saldoApos;

    public NovoLancamento(int contaId, char natureza, String descricao, BigDecimal valor, BigDecimal saldoApos) {
        this.contaId = contaId;
        this.natureza = natureza;
        this.descricao = descricao;
        this.valor = valor;
        this.saldoApos = saldoApos;
    }

    public int contaId() {
        return contaId;
    }

    public char natureza() {
        return natureza;
    }

    public String descricao() {
        return descricao;
    }

    public BigDecimal valor() {
        return valor;
    }

    public BigDecimal saldoApos() {
        return saldoApos;
    }
}
