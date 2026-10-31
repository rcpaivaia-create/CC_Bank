package br.com.ccbank.servidor.dados;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public final class RepositorioLancamentos {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    /**
     * Os N mais recentes do período, reordenados do mais antigo para o mais novo. A ordem é
     * pelo id: ele é gerado depois que a conta foi travada, enquanto {@code data_hora} (now())
     * marca o início da transação e pode inverter operações concorrentes na mesma conta.
     */
    private static final String SQL_LISTAR =
            "select data_hora, descricao, natureza, valor, saldo_apos, contraparte from ("
                    + " select l.id, l.data_hora, l.descricao, l.natureza, l.valor, l.saldo_apos,"
                    + "  (select co.codigo || '/' || ct.numero from lancamento l2"
                    + "    join conta ct on ct.id = l2.conta_id"
                    + "    join cooperativa co on co.id = ct.cooperativa_id"
                    + "    where l2.transacao_id = l.transacao_id and l2.id <> l.id limit 1) as contraparte"
                    + " from lancamento l"
                    + " where l.conta_id = ? and l.data_hora >= ?::date and l.data_hora < ?::date + 1"
                    + " order by l.id desc limit ?"
                    + ") recentes order by id";

    private RepositorioLancamentos() {
    }

    public static List<LancamentoExtrato> listar(Connection c, int contaId, LocalDate inicio, LocalDate fim, int limite)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SQL_LISTAR)) {
            ps.setInt(1, contaId);
            ps.setObject(2, inicio);
            ps.setObject(3, fim);
            ps.setInt(4, limite);
            List<LancamentoExtrato> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String contraparte = rs.getString(6);
                    lista.add(new LancamentoExtrato(
                            rs.getObject(1, OffsetDateTime.class).atZoneSameInstant(FUSO).toLocalDateTime(),
                            rs.getString(2), rs.getString(3).charAt(0), rs.getBigDecimal(4), rs.getBigDecimal(5),
                            contraparte == null ? "" : contraparte));
                }
            }
            return lista;
        }
    }

    public static void inserir(Connection c, int contaId, long transacaoId, char natureza, String descricao,
            BigDecimal valor, BigDecimal saldoApos) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "insert into lancamento (conta_id, transacao_id, natureza, descricao, valor, saldo_apos)"
                        + " values (?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, contaId);
            ps.setLong(2, transacaoId);
            ps.setString(3, String.valueOf(natureza));
            ps.setString(4, descricao);
            ps.setBigDecimal(5, valor);
            ps.setBigDecimal(6, saldoApos);
            ps.executeUpdate();
        }
    }
}
