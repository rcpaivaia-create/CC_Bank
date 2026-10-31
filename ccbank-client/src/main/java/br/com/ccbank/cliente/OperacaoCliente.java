package br.com.ccbank.cliente;

import java.io.IOException;

/** Operação executada depois de a senha do cliente ser validada. */
@FunctionalInterface
public interface OperacaoCliente {

    Resposta executar() throws IOException;
}
