package br.com.ccbank.servidor.negocio;

import java.sql.Connection;
import java.sql.SQLException;

import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;
import br.com.ccbank.servidor.dados.Conta;
import br.com.ccbank.servidor.dados.RegistroJornal;

/**
 * Confere a senha do cliente antes de uma operação. Não altera saldo nem gera lançamento.
 * v1: o resultado não fica guardado; a operação seguinte não verifica se houve validação.
 */
final class OperacaoValidarSenha implements Operacao {

    @Override
    public String mensagemSucesso() {
        return "Senha validada";
    }

    @Override
    public Mensagem executar(Connection c, Mensagem req, RegistroJornal jornal) throws Recusa, SQLException {
        Regras.exigir(req, Regras.ORIGEM);
        Regras.exigir(req, Rotulo.SENHA_CONTA);
        Conta conta = Regras.localizarConta(c, req, jornal);
        Regras.conferirSenha(conta, req);
        return new Mensagem().adicionar(Rotulo.TITULAR, conta.titular());
    }
}
