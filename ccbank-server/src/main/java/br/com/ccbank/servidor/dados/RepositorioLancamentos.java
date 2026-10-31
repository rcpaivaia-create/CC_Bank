package br.com.ccbank.servidor.dados;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class RepositorioLancamentos {

    private RepositorioLancamentos() {
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
