package br.com.ccbank.interceptador.mediacao;

import br.com.ccbank.interceptador.regras.Sentido;
import br.com.ccbank.protocolo.Mensagem;

/** Observa cada troca decodificável do proxy (para histórico). O headless usa {@link #NULO}. */
public interface OuvinteTroca {

    void observar(Sentido sentido, Mensagem original, DecisaoMediacao decisao);

    OuvinteTroca NULO = (sentido, original, decisao) -> {
    };
}
