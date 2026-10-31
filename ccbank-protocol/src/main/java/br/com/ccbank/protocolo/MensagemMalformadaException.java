package br.com.ccbank.protocolo;

import java.io.IOException;

/** Bytes recebidos não formam uma mensagem válida do protocolo. */
public class MensagemMalformadaException extends IOException {

    private static final long serialVersionUID = 1L;

    public MensagemMalformadaException(String mensagem) {
        super(mensagem);
    }
}
