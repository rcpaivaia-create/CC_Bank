package br.com.ccbank.servidor.dados;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class RepositorioOperadores {

    private static final String SQL_POR_LOGIN =
            "select o.id, o.login, o.senha, o.nome, co.codigo, p.codigo, t.codigo, o.ativo"
                    + " from operador o"
                    + " join terminal t on t.id = o.terminal_id"
                    + " join pac p on p.id = t.pac_id"
                    + " join cooperativa co on co.id = p.cooperativa_id"
                    + " where o.login = ?";

    private RepositorioOperadores() {
    }

    public static Optional<Operador> buscarPorLogin(Connection c, String login) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SQL_POR_LOGIN)) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Operador(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getString(7), rs.getBoolean(8)));
            }
        }
    }
}
