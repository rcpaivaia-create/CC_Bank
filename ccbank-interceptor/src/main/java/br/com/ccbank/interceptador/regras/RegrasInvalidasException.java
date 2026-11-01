package br.com.ccbank.interceptador.regras;

import java.io.IOException;

/** Arquivo de regras inválido: JSON malformado, campo faltando ou valor desconhecido. */
public final class RegrasInvalidasException extends IOException {

    private static final long serialVersionUID = 1L;

    public RegrasInvalidasException(String mensagem) {
        super(mensagem);
    }
}
