package br.com.ccbank.servidor.negocio;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import br.com.ccbank.protocolo.CodigoRetorno;
import br.com.ccbank.protocolo.FormatoValores;
import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;
import br.com.ccbank.servidor.dados.Conta;
import br.com.ccbank.servidor.dados.NovoLancamento;
import br.com.ccbank.servidor.dados.RegistroJornal;
import br.com.ccbank.servidor.dados.RepositorioContas;

final class OperacaoTransferencia implements Operacao {

    @Override
    public String mensagemSucesso() {
        return "Transferência efetuada";
    }

    @Override
    public Mensagem executar(Connection c, Mensagem req, RegistroJornal jornal) throws Recusa, SQLException {
        Regras.exigir(req, Regras.ORIGEM);
        Regras.exigir(req, Rotulo.COOPERATIVA_DESTINO, Rotulo.CONTA_DESTINO, Rotulo.VALOR);
        BigDecimal valor = Regras.valorOperacao(req, jornal);

        String coopDestino = Regras.valor(req, Rotulo.COOPERATIVA_DESTINO);
        String numeroDestino = Regras.valor(req, Rotulo.CONTA_DESTINO);
        jornal.setCooperativaDestino(coopDestino);
        jornal.setContaDestino(numeroDestino);
        if (coopDestino.equals(Regras.valor(req, Rotulo.COOPERATIVA_CONTA))
                && numeroDestino.equals(Regras.valor(req, Rotulo.CONTA))) {
            throw new Recusa(CodigoRetorno.CONTA_DESTINO_IGUAL);
        }

        Conta origem = Regras.localizarConta(c, req, jornal);
        Optional<Conta> destino = RepositorioContas.buscar(c, coopDestino, numeroDestino);
        if (!destino.isPresent()) {
            throw new Recusa(CodigoRetorno.CONTA_INEXISTENTE);
        }
        if (destino.get().id() == origem.id()) {
            throw new Recusa(CodigoRetorno.CONTA_DESTINO_IGUAL);
        }
        jornal.setContaDestinoId(destino.get().id());

        // Trava sempre na ordem crescente de id: transferências cruzadas não se bloqueiam mutuamente.
        int menor = Math.min(origem.id(), destino.get().id());
        int maior = Math.max(origem.id(), destino.get().id());
        Conta travadaMenor = RepositorioContas.buscarParaAtualizar(c, menor);
        Conta travadaMaior = RepositorioContas.buscarParaAtualizar(c, maior);
        Conta de = travadaMenor.id() == origem.id() ? travadaMenor : travadaMaior;
        Conta para = de == travadaMenor ? travadaMaior : travadaMenor;

        if (de.saldo().compareTo(valor) < 0) {
            throw new Recusa(CodigoRetorno.SALDO_INSUFICIENTE);
        }
        BigDecimal saldoDe = de.saldo().subtract(valor);
        BigDecimal saldoPara = para.saldo().add(valor);
        RepositorioContas.atualizarSaldo(c, de.id(), saldoDe);
        RepositorioContas.atualizarSaldo(c, para.id(), saldoPara);
        jornal.setSaldoAposOrigem(saldoDe);
        jornal.setSaldoAposDestino(saldoPara);
        jornal.adicionarLancamento(new NovoLancamento(de.id(), 'D', "TRANSFERENCIA ENVIADA", valor, saldoDe));
        jornal.adicionarLancamento(new NovoLancamento(para.id(), 'C', "TRANSFERENCIA RECEBIDA", valor, saldoPara));
        return new Mensagem()
                .adicionar(Rotulo.SALDO, FormatoValores.formatarValor(saldoDe))
                .adicionar(Rotulo.TITULAR, de.titular());
    }
}
