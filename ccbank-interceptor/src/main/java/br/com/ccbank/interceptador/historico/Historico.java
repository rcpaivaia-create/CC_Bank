package br.com.ccbank.interceptador.historico;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import br.com.ccbank.interceptador.mediacao.DecisaoMediacao;
import br.com.ccbank.interceptador.mediacao.OuvinteTroca;
import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;
import br.com.ccbank.interceptador.regras.Sentido;

/** Histórico de trocas de uma aba. É o {@link OuvinteTroca} que a aba liga ao proxy. */
public final class Historico implements OuvinteTroca {

    private final List<TrocaRegistrada> trocas = new CopyOnWriteArrayList<>();

    @Override
    public void observar(Sentido sentido, Mensagem original, DecisaoMediacao decisao) {
        String transacao = original.primeiro(Rotulo.TRANSACAO.codigo()).orElse("");
        boolean adulterada = decisao.reserializar();
        Mensagem resultante = decisao.resultado() != null ? decisao.resultado().mensagem() : original;
        trocas.add(new TrocaRegistrada(LocalDateTime.now(), sentido, transacao, adulterada, decisao.acao(),
                original, resultante));
    }

    public List<TrocaRegistrada> trocas() {
        return Collections.unmodifiableList(new ArrayList<>(trocas));
    }

    public void limpar() {
        trocas.clear();
    }
}
