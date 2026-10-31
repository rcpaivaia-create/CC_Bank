package br.com.ccbank.servidor.negocio;

import java.sql.Connection;
import java.sql.SQLException;

import br.com.ccbank.protocolo.FormatoValores;
import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;
import br.com.ccbank.servidor.dados.Conta;
import br.com.ccbank.servidor.dados.RegistroJornal;

final class OperacaoSaldo implements Operacao {

    @Override
    public String mensagemSucesso() {
        return "Saldo consultado";
    }

    @Override
    public Mensagem executar(Connection c, Mensagem req, RegistroJornal jornal) throws Recusa, SQLException {
        Regras.exigir(req, Regras.ORIGEM);
        Conta conta = Regras.localizarConta(c, req, jornal);
        return new Mensagem()
                .adicionar(Rotulo.SALDO, FormatoValores.formatarValor(conta.saldo()))
                .adicionar(Rotulo.TITULAR, conta.titular());
    }
}
