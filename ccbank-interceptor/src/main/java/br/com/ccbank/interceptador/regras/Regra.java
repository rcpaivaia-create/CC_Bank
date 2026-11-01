package br.com.ccbank.interceptador.regras;

import java.util.ArrayList;
import java.util.List;

/** Uma regra do interceptador: condição que seleciona a mensagem e ações que a adulteram. */
public final class Regra {

    private final String nome;
    private final boolean ativa;
    private final Sentido sentido;
    private final Condicao condicao;
    private final List<Acao> acoes;

    public Regra(String nome, boolean ativa, Sentido sentido, Condicao condicao, List<Acao> acoes) {
        this.nome = nome;
        this.ativa = ativa;
        this.sentido = sentido;
        this.condicao = condicao;
        this.acoes = new ArrayList<>(acoes);
    }

    public String nome() {
        return nome;
    }

    public boolean ativa() {
        return ativa;
    }

    public Sentido sentido() {
        return sentido;
    }

    public Condicao condicao() {
        return condicao;
    }

    public List<Acao> acoes() {
        return new ArrayList<>(acoes);
    }
}
