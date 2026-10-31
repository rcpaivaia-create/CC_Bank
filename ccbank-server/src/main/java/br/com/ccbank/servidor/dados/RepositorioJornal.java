package br.com.ccbank.servidor.dados;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public final class RepositorioJornal {

    private static final String SQL_INSERIR =
            "insert into transacao (tipo, operador_login, cooperativa, pac, terminal,"
                    + " conta_origem_id, cooperativa_origem, conta_origem,"
                    + " conta_destino_id, cooperativa_destino, conta_destino,"
                    + " valor, saldo_apos_origem, saldo_apos_destino,"
                    + " codigo_retorno, intercredis, ip_origem, duracao_ms)"
                    + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) returning id";

    private RepositorioJornal() {
    }

    /** @return o id da linha inserida */
    public static long inserir(Connection c, RegistroJornal r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SQL_INSERIR)) {
            int i = 1;
            ps.setString(i++, r.getTipo());
            ps.setString(i++, r.getOperadorLogin());
            ps.setString(i++, r.getCooperativa());
            ps.setString(i++, r.getPac());
            ps.setString(i++, r.getTerminal());
            setInteger(ps, i++, r.getContaOrigemId());
            ps.setString(i++, r.getCooperativaOrigem());
            ps.setString(i++, r.getContaOrigem());
            setInteger(ps, i++, r.getContaDestinoId());
            ps.setString(i++, r.getCooperativaDestino());
            ps.setString(i++, r.getContaDestino());
            setDecimal(ps, i++, r.getValor());
            setDecimal(ps, i++, r.getSaldoAposOrigem());
            setDecimal(ps, i++, r.getSaldoAposDestino());
            ps.setString(i++, r.getCodigoRetorno());
            ps.setBoolean(i++, r.isIntercredis());
            ps.setString(i++, r.getIpOrigem());
            setInteger(ps, i, r.getDuracaoMs());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private static void setInteger(PreparedStatement ps, int i, Integer v) throws SQLException {
        if (v == null) {
            ps.setNull(i, Types.INTEGER);
        } else {
            ps.setInt(i, v);
        }
    }

    private static void setDecimal(PreparedStatement ps, int i, BigDecimal v) throws SQLException {
        if (v == null) {
            ps.setNull(i, Types.NUMERIC);
        } else {
            ps.setBigDecimal(i, v);
        }
    }
}
