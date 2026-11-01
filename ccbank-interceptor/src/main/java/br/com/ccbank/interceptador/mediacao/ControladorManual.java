package br.com.ccbank.interceptador.mediacao;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.SynchronousQueue;

import br.com.ccbank.interceptador.regras.ResultadoAplicacao;
import br.com.ccbank.interceptador.regras.Sentido;
import br.com.ccbank.protocolo.Mensagem;

/**
 * Modo manual: retém a mensagem e bloqueia a thread do par até a tela resolver
 * ({@link #encaminhar}, {@link #encaminharOriginal} ou {@link #descartar}). O sentido fora do
 * filtro passa como passagem, sem reter.
 */
public final class ControladorManual implements Mediador {

    public enum Filtro {
        REQUISICOES, RESPOSTAS, AMBAS
    }

    private volatile Filtro filtro;
    private volatile Mensagem retida;
    private volatile Sentido sentidoRetido;
    private volatile Runnable aoMudarRetencao = () -> {
    };
    private final SynchronousQueue<DecisaoMediacao> resolucao = new SynchronousQueue<>();

    public ControladorManual(Filtro filtro) {
        this.filtro = filtro;
    }

    public void filtro(Filtro filtro) {
        this.filtro = filtro;
    }

    public Filtro filtro() {
        return filtro;
    }

    public void aoMudarRetencao(Runnable aoMudar) {
        this.aoMudarRetencao = aoMudar;
    }

    public Optional<Mensagem> retida() {
        return Optional.ofNullable(retida);
    }

    public Sentido sentidoRetido() {
        return sentidoRetido;
    }

    @Override
    public DecisaoMediacao intervir(Mensagem mensagem, Sentido sentido) {
        if (!retemSentido(sentido)) {
            return DecisaoMediacao.encaminharOriginal(mensagem);
        }
        this.retida = mensagem;
        this.sentidoRetido = sentido;
        aoMudarRetencao.run();
        try {
            return resolucao.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return DecisaoMediacao.descartar();
        } finally {
            this.retida = null;
            this.sentidoRetido = null;
            aoMudarRetencao.run();
        }
    }

    public void encaminhar(Mensagem editada) {
        resolver(DecisaoMediacao.encaminharModificada(new ResultadoAplicacao(editada, Collections.emptyList())));
    }

    public void encaminharOriginal() {
        Mensagem original = retida;
        resolver(DecisaoMediacao.encaminharOriginal(original));
    }

    public void descartar() {
        resolver(DecisaoMediacao.descartar());
    }

    private void resolver(DecisaoMediacao decisao) {
        // offer() não bloqueia: entrega só se a thread do par já estiver esperando em take()
        // (o que ocorre sempre que há uma mensagem retida). Chamado da EDT, nunca pode travar.
        resolucao.offer(decisao);
    }

    private boolean retemSentido(Sentido sentido) {
        if (filtro == Filtro.AMBAS) {
            return true;
        }
        return sentido == Sentido.REQUISICAO ? filtro == Filtro.REQUISICOES : filtro == Filtro.RESPOSTAS;
    }
}
