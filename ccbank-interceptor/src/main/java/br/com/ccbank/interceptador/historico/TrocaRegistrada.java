package br.com.ccbank.interceptador.historico;

import java.time.LocalDateTime;

import br.com.ccbank.interceptador.mediacao.DecisaoMediacao;
import br.com.ccbank.interceptador.regras.Sentido;
import br.com.ccbank.protocolo.Mensagem;

/** Uma troca registrada no histórico de uma aba. */
public final class TrocaRegistrada {

    private final LocalDateTime hora;
    private final Sentido sentido;
    private final String transacao;
    private final boolean adulterada;
    private final DecisaoMediacao.Acao acao;
    private final Mensagem original;
    private final Mensagem resultante;

    public TrocaRegistrada(LocalDateTime hora, Sentido sentido, String transacao, boolean adulterada,
            DecisaoMediacao.Acao acao, Mensagem original, Mensagem resultante) {
        this.hora = hora;
        this.sentido = sentido;
        this.transacao = transacao;
        this.adulterada = adulterada;
        this.acao = acao;
        this.original = original;
        this.resultante = resultante;
    }

    public LocalDateTime hora() {
        return hora;
    }

    public Sentido sentido() {
        return sentido;
    }

    public String transacao() {
        return transacao;
    }

    public boolean adulterada() {
        return adulterada;
    }

    public DecisaoMediacao.Acao acao() {
        return acao;
    }

    public Mensagem original() {
        return original;
    }

    public Mensagem resultante() {
        return resultante;
    }
}
