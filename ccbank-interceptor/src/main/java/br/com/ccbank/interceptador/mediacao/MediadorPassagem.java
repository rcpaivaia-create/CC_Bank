package br.com.ccbank.interceptador.mediacao;

import br.com.ccbank.interceptador.regras.Sentido;
import br.com.ccbank.protocolo.Mensagem;

/** Observa e encaminha sem alterar. */
public final class MediadorPassagem implements Mediador {

    @Override
    public DecisaoMediacao intervir(Mensagem mensagem, Sentido sentido) {
        return DecisaoMediacao.encaminharOriginal(mensagem);
    }
}
