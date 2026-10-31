package br.com.ccbank.servidor.dados;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Linha do extrato. {@code contraparte} é {@code cccc/nnnnnn} ou vazio. */
public final class LancamentoExtrato {

    private final LocalDateTime dataHora;
    private final String descricao;
    private final char natureza;
    private final BigDecimal valor;
    private final BigDecimal saldoApos;
    private final String contraparte;

    public LancamentoExtrato(LocalDateTime dataHora, String descricao, char natureza,
            BigDecimal valor, BigDecimal saldoApos, String contraparte) {
        this.dataHora = dataHora;
        this.descricao = descricao;
        this.natureza = natureza;
        this.valor = valor;
        this.saldoApos = saldoApos;
        this.contraparte = contraparte;
    }

    public LocalDateTime dataHora() {
        return dataHora;
    }

    public String descricao() {
        return descricao;
    }

    public char natureza() {
        return natureza;
    }

    public BigDecimal valor() {
        return valor;
    }

    public BigDecimal saldoApos() {
        return saldoApos;
    }

    public String contraparte() {
        return contraparte;
    }
}
