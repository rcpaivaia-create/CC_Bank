package br.com.ccbank.servidor.dados;

import java.math.BigDecimal;

/** Conta com o código da cooperativa e o nome do titular. */
public final class Conta {

    private final int id;
    private final String cooperativa;
    private final String numero;
    private final String titular;
    private final String senha;
    private final BigDecimal saldo;

    public Conta(int id, String cooperativa, String numero, String titular, String senha, BigDecimal saldo) {
        this.id = id;
        this.cooperativa = cooperativa;
        this.numero = numero;
        this.titular = titular;
        this.senha = senha;
        this.saldo = saldo;
    }

    public int id() {
        return id;
    }

    public String cooperativa() {
        return cooperativa;
    }

    public String numero() {
        return numero;
    }

    public String titular() {
        return titular;
    }

    public String senha() {
        return senha;
    }

    public BigDecimal saldo() {
        return saldo;
    }
}
