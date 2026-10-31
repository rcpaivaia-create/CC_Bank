package br.com.ccbank.protocolo;

import java.io.IOException;

/** Mensagem ultrapassou {@link CodecMensagem#TAMANHO_MAXIMO} bytes. */
public class MensagemMuitoGrandeException extends IOException {

    private static final long serialVersionUID = 1L;

    public MensagemMuitoGrandeException(String mensagem) {
        super(mensagem);
    }
}
