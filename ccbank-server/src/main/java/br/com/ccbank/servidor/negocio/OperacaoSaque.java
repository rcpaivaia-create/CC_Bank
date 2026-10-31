package br.com.ccbank.servidor.negocio;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

import br.com.ccbank.protocolo.CodigoRetorno;
import br.com.ccbank.protocolo.FormatoValores;
import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;
import br.com.ccbank.servidor.dados.Conta;
import br.com.ccbank.servidor.dados.NovoLancamento;
import br.com.ccbank.servidor.dados.RegistroJornal;
import br.com.ccbank.servidor.dados.RepositorioContas;

final class OperacaoSaque implements Operacao {

    @Override
    public String mensagemSucesso() {
        return "Saque efetuado";
    }

    @Override
    public Mensagem executar(Connection c, Mensagem req, RegistroJornal jornal) throws Recusa, SQLException {
        Regras.exigir(req, Regras.ORIGEM);
        Regras.exigir(req, Rotulo.VALOR);
        BigDecimal valor = Regras.valorOperacao(req, jornal);
        Conta conta = RepositorioContas.buscarParaAtualizar(c, Regras.localizarConta(c, req, jornal).id());
        if (conta.saldo().compareTo(valor) < 0) {
            throw new Recusa(CodigoRetorno.SALDO_INSUFICIENTE);
        }
        BigDecimal saldo = conta.saldo().subtract(valor);
        RepositorioContas.atualizarSaldo(c, conta.id(), saldo);
        jornal.setSaldoAposOrigem(saldo);
        jornal.adicionarLancamento(new NovoLancamento(conta.id(), 'D', "SAQUE", valor, saldo));
        return new Mensagem()
                .adicionar(Rotulo.SALDO, FormatoValores.formatarValor(saldo))
                .adicionar(Rotulo.TITULAR, conta.titular());
    }
}
