package br.com.ccbank.cliente;

import java.io.IOException;

/**
 * A senha foi validada (a validação já está no jornal do servidor), mas a operação falhou na
 * comunicação. Carrega a resposta da validação para a tela poder mostrá-la.
 */
public class FalhaAposValidacaoException extends IOException {

    private static final long serialVersionUID = 1L;

    private final transient Resposta validacao;

    public FalhaAposValidacaoException(Resposta validacao, IOException causa) {
        super(causa.getMessage(), causa);
        this.validacao = validacao;
    }

    public Resposta validacao() {
        return validacao;
    }
}
