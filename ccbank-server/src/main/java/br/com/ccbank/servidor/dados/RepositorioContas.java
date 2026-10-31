package br.com.ccbank.servidor.dados;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class RepositorioContas {

    private static final String COLUNAS =
            "select ct.id, co.codigo, ct.numero, cl.nome, ct.senha, ct.saldo"
                    + " from conta ct"
                    + " join cooperativa co on co.id = ct.cooperativa_id"
                    + " join cliente cl on cl.id = ct.cliente_id";

    private RepositorioContas() {
    }

    public static Optional<Conta> buscar(Connection c, String cooperativa, String numero) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(COLUNAS + " where co.codigo = ? and ct.numero = ?")) {
            ps.setString(1, cooperativa);
            ps.setString(2, numero);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(ler(rs)) : Optional.empty();
            }
        }
    }

    /** Relê a conta travando a linha até o fim da transação. */
    public static Conta buscarParaAtualizar(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(COLUNAS + " where ct.id = ? for update of ct")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("conta " + id + " desapareceu durante a transação");
                }
                return ler(rs);
            }
        }
    }

    public static void atualizarSaldo(Connection c, int id, BigDecimal saldo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("update conta set saldo = ? where id = ?")) {
            ps.setBigDecimal(1, saldo);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    private static Conta ler(ResultSet rs) throws SQLException {
        return new Conta(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getString(5), rs.getBigDecimal(6));
    }
}
