package br.com.ccbank.servidor.negocio;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

import br.com.ccbank.protocolo.FormatoValores;
import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;
import br.com.ccbank.servidor.dados.Conta;
import br.com.ccbank.servidor.dados.NovoLancamento;
import br.com.ccbank.servidor.dados.RegistroJornal;
import br.com.ccbank.servidor.dados.RepositorioContas;

final class OperacaoDeposito implements Operacao {

    @Override
    public String mensagemSucesso() {
        return "Depósito efetuado";
    }

    @Override
    public Mensagem executar(Connection c, Mensagem req, RegistroJornal jornal) throws Recusa, SQLException {
        Regras.exigir(req, Regras.ORIGEM);
        Regras.exigir(req, Rotulo.VALOR);
        BigDecimal valor = Regras.valorOperacao(req, jornal);
        Conta conta = RepositorioContas.buscarParaAtualizar(c, Regras.localizarConta(c, req, jornal).id());
        BigDecimal saldo = conta.saldo().add(valor);
        RepositorioContas.atualizarSaldo(c, conta.id(), saldo);
        jornal.setSaldoAposOrigem(saldo);
        jornal.adicionarLancamento(new NovoLancamento(conta.id(), 'C', "DEPOSITO", valor, saldo));
        return new Mensagem()
                .adicionar(Rotulo.SALDO, FormatoValores.formatarValor(saldo))
                .adicionar(Rotulo.TITULAR, conta.titular());
    }
}
