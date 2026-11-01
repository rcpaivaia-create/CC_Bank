package br.com.ccbank.interceptador.mediacao;

import br.com.ccbank.interceptador.regras.Sentido;
import br.com.ccbank.protocolo.Mensagem;

/** Decide o que o proxy faz com cada mensagem decodificável, conforme o modo do interceptador. */
public interface Mediador {

    DecisaoMediacao intervir(Mensagem mensagem, Sentido sentido);
}
