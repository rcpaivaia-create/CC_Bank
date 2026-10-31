package br.com.ccbank.servidor.negocio;

import br.com.ccbank.protocolo.CodigoRetorno;

/** Recusa de negócio: a transação é desfeita e o código vai na resposta e no jornal. */
public class Recusa extends Exception {

    private static final long serialVersionUID = 1L;

    private final CodigoRetorno codigo;

    public Recusa(CodigoRetorno codigo) {
        super(codigo.descricao(), null, false, false);
        this.codigo = codigo;
    }

    public CodigoRetorno codigo() {
        return codigo;
    }
}
