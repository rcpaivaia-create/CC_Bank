package br.com.ccbank.interceptador.regras;

import java.util.ArrayList;
import java.util.List;

import br.com.ccbank.protocolo.Mensagem;

/** Resultado de aplicar o motor de regras: a mensagem resultante e o que foi adulterado. */
public final class ResultadoAplicacao {

    private final Mensagem mensagem;
    private final List<Adulteracao> adulteracoes;

    public ResultadoAplicacao(Mensagem mensagem, List<Adulteracao> adulteracoes) {
        this.mensagem = mensagem;
        this.adulteracoes = new ArrayList<>(adulteracoes);
    }

    public Mensagem mensagem() {
        return mensagem;
    }

    public List<Adulteracao> adulteracoes() {
        return new ArrayList<>(adulteracoes);
    }

    public boolean houveAdulteracao() {
        return !adulteracoes.isEmpty();
    }
}
