package br.com.ccbank.servidor.negocio;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import br.com.ccbank.protocolo.CodigoRetorno;
import br.com.ccbank.protocolo.FormatoValores;
import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;
import br.com.ccbank.servidor.dados.Conta;
import br.com.ccbank.servidor.dados.LancamentoExtrato;
import br.com.ccbank.servidor.dados.RegistroJornal;
import br.com.ccbank.servidor.dados.RepositorioLancamentos;

/** Extrato por período: padrão últimos 30 dias, no máximo 200 lançamentos. */
final class OperacaoExtrato implements Operacao {

    static final int DIAS_PADRAO = 30;
    static final int LIMITE = 200;
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Override
    public String mensagemSucesso() {
        return "Extrato gerado";
    }

    @Override
    public Mensagem executar(Connection c, Mensagem req, RegistroJornal jornal) throws Recusa, SQLException {
        Regras.exigir(req, Regras.ORIGEM);
        LocalDate fim = data(req, Rotulo.DATA_FIM);
        if (fim == null) {
            fim = LocalDate.now(FUSO);
        }
        LocalDate inicio = data(req, Rotulo.DATA_INICIO);
        if (inicio == null) {
            inicio = fim.minusDays(DIAS_PADRAO);
        }
        if (inicio.isAfter(fim)) {
            throw new Recusa(CodigoRetorno.MENSAGEM_MALFORMADA);
        }

        Conta conta = Regras.localizarConta(c, req, jornal);
        List<LancamentoExtrato> lancamentos = RepositorioLancamentos.listar(c, conta.id(), inicio, fim, LIMITE);
        Mensagem m = new Mensagem()
                .adicionar(Rotulo.TITULAR, conta.titular())
                .adicionar(Rotulo.QTD_LANCAMENTOS, String.valueOf(lancamentos.size()));
        for (LancamentoExtrato l : lancamentos) {
            m.adicionar(Rotulo.LANC_DATA_HORA, FormatoValores.formatarDataHora(l.dataHora()))
                    .adicionar(Rotulo.LANC_TIPO, l.descricao())
                    .adicionar(Rotulo.LANC_VALOR, FormatoValores.formatarValor(l.natureza() == 'D' ? l.valor().negate() : l.valor()))
                    .adicionar(Rotulo.LANC_SALDO_APOS, FormatoValores.formatarValor(l.saldoApos()))
                    .adicionar(Rotulo.LANC_CONTRAPARTE, l.contraparte());
        }
        return m;
    }

    /** @return a data informada, {@code null} se ausente; recusa com 006 se malformada */
    private static LocalDate data(Mensagem req, Rotulo rotulo) throws Recusa {
        Optional<String> texto = req.primeiro(rotulo);
        if (!texto.isPresent()) {
            return null;
        }
        Optional<LocalDate> data = FormatoValores.parseData(texto.get());
        if (!data.isPresent()) {
            throw new Recusa(CodigoRetorno.MENSAGEM_MALFORMADA);
        }
        return data.get();
    }
}
