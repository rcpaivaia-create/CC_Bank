package br.com.ccbank.cliente;

import java.io.IOException;

/** Não foi possível conectar ao servidor (ou ao interceptador). */
public class ServicoIndisponivelException extends IOException {

    private static final long serialVersionUID = 1L;

    public ServicoIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
