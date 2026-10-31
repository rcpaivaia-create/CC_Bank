package br.com.ccbank.servidor.negocio;

import java.sql.Connection;
import java.sql.SQLException;

import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.servidor.dados.RegistroJornal;

/**
 * Uma transação do protocolo. Executa dentro da transação de banco aberta pelo
 * {@link ProcessadorTransacoes}, preenchendo o jornal conforme avança.
 */
public interface Operacao {

    /** Texto do rótulo 00091 em caso de sucesso. */
    String mensagemSucesso();

    /**
     * @return os campos específicos da resposta (sem 00090, 00091 e 00094, que o processador acrescenta)
     * @throws Recusa recusa de negócio; o processador desfaz a transação
     */
    Mensagem executar(Connection c, Mensagem req, RegistroJornal jornal) throws Recusa, SQLException;
}
