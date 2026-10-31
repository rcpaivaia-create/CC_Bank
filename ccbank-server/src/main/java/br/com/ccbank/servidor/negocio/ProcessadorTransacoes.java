package br.com.ccbank.servidor.negocio;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.ccbank.protocolo.Campo;
import br.com.ccbank.protocolo.CodigoRetorno;
import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;
import br.com.ccbank.servidor.dados.NovoLancamento;
import br.com.ccbank.servidor.dados.RegistroJornal;
import br.com.ccbank.servidor.dados.RepositorioJornal;
import br.com.ccbank.servidor.dados.RepositorioLancamentos;

/**
 * Despacha cada requisição para a sua {@link Operacao}, numa transação de banco, e grava
 * uma linha no jornal para toda requisição, inclusive as recusadas.
 *
 * <p>v1: confia nos campos de operador, cooperativa, PAC e terminal da mensagem.
 */
public final class ProcessadorTransacoes {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessadorTransacoes.class);

    private final DataSource ds;
    private final Map<String, Operacao> operacoes = new HashMap<>();

    public ProcessadorTransacoes(DataSource ds) {
        this.ds = ds;
        operacoes.put("login", new OperacaoLogin());
        operacoes.put("saldo", new OperacaoSaldo());
        operacoes.put("saque", new OperacaoSaque());
        operacoes.put("deposito", new OperacaoDeposito());
        operacoes.put("transferencia", new OperacaoTransferencia());
    }

    public Mensagem processar(Mensagem req, String ipOrigem) {
        long inicio = System.nanoTime();
        RegistroJornal jornal = new RegistroJornal();
        jornal.setOperadorLogin(req.primeiro(Rotulo.OPERADOR).orElse(null));
        jornal.setCooperativa(req.primeiro(Rotulo.COOPERATIVA_OPERADOR).orElse(null));
        jornal.setPac(req.primeiro(Rotulo.PAC).orElse(null));
        jornal.setTerminal(req.primeiro(Rotulo.TERMINAL).orElse(null));
        jornal.setIpOrigem(ipOrigem);

        String transacao = req.primeiro(Rotulo.TRANSACAO).orElse(null);
        Operacao op = transacao == null ? null : operacoes.get(transacao);
        jornal.setTipo(op == null ? "INVALIDA" : transacao.toUpperCase(Locale.ROOT));

        try (Connection c = ds.getConnection()) {
            if (op == null) {
                return recusar(c, jornal, CodigoRetorno.MENSAGEM_MALFORMADA, inicio);
            }
            c.setAutoCommit(false);
            try {
                Mensagem corpo = op.executar(c, req, jornal);
                jornal.setCodigoRetorno(CodigoRetorno.SUCESSO.codigo());
                jornal.setDuracaoMs(decorrido(inicio));
                long id = RepositorioJornal.inserir(c, jornal);
                for (NovoLancamento l : jornal.getLancamentos()) {
                    RepositorioLancamentos.inserir(c, l.contaId(), id, l.natureza(), l.descricao(), l.valor(), l.saldoApos());
                }
                c.commit();
                return resposta(CodigoRetorno.SUCESSO, op.mensagemSucesso(), corpo, id);
            } catch (Recusa r) {
                c.rollback();
                c.setAutoCommit(true);
                return recusar(c, jornal, r.codigo(), inicio);
            } catch (SQLException | RuntimeException e) {
                LOG.error("Erro ao processar {}", transacao, e);
                c.rollback();
                c.setAutoCommit(true);
                return recusar(c, jornal, CodigoRetorno.ERRO_INTERNO, inicio);
            }
        } catch (SQLException e) {
            LOG.error("Erro de banco ao processar {}", transacao, e);
            return resposta(CodigoRetorno.ERRO_INTERNO, CodigoRetorno.ERRO_INTERNO.descricao(), new Mensagem(), null);
        }
    }

    /** Grava o jornal da recusa (fora da transação desfeita) e monta a resposta. */
    private static Mensagem recusar(Connection c, RegistroJornal jornal, CodigoRetorno codigo, long inicio)
            throws SQLException {
        jornal.setCodigoRetorno(codigo.codigo());
        jornal.setDuracaoMs(decorrido(inicio));
        long id = RepositorioJornal.inserir(c, jornal);
        return resposta(codigo, codigo.descricao(), new Mensagem(), id);
    }

    private static Mensagem resposta(CodigoRetorno codigo, String texto, Mensagem corpo, Long idJornal) {
        Mensagem m = new Mensagem()
                .adicionar(Rotulo.CODIGO_RETORNO, codigo.codigo())
                .adicionar(Rotulo.MENSAGEM_RETORNO, texto);
        for (Campo campo : corpo.campos()) {
            m.adicionar(campo.rotulo(), campo.valor());
        }
        if (idJornal != null) {
            m.adicionar(Rotulo.ID_TRANSACAO, String.valueOf(idJornal));
        }
        return m;
    }

    private static int decorrido(long inicio) {
        return (int) ((System.nanoTime() - inicio) / 1_000_000);
    }
}
