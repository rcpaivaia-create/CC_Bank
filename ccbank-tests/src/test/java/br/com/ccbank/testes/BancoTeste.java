package br.com.ccbank.testes;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumMap;
import java.util.Map;

import javax.sql.DataSource;

import br.com.ccbank.servidor.config.Ambiente;
import br.com.ccbank.servidor.config.Caminhos;
import br.com.ccbank.servidor.config.ConfiguracaoBanco;
import br.com.ccbank.servidor.dados.SetupBanco;

/** Recria os bancos de teste e oferece consultas auxiliares para os ITs. */
final class BancoTeste {

    private static final Map<Ambiente, DataSource> POOLS = new EnumMap<>(Ambiente.class);
    private static ConfiguracaoBanco cfg;

    private BancoTeste() {
    }

    static synchronized DataSource recriar(Ambiente amb) throws Exception {
        if (cfg == null) {
            cfg = ConfiguracaoBanco.carregar(ConfiguracaoBanco.arquivoPadrao());
        }
        SetupBanco setup = new SetupBanco(cfg);
        setup.criarBancoSeNaoExistir(amb.bancoTeste());
        setup.recriarSchema(amb.bancoTeste(), Caminhos.localizar(amb.pastaScripts()));
        return POOLS.computeIfAbsent(amb, a -> cfg.criarPool(a.bancoTeste(), 20));
    }

    static BigDecimal saldo(DataSource ds, String coop, String conta) throws SQLException {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "select ct.saldo from conta ct join cooperativa co on co.id = ct.cooperativa_id"
                                + " where co.codigo = ? and ct.numero = ?")) {
            ps.setString(1, coop);
            ps.setString(2, conta);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("conta inexistente: " + coop + "/" + conta);
                }
                return rs.getBigDecimal(1);
            }
        }
    }

    static String titular(DataSource ds, String coop, String conta) throws SQLException {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "select cl.nome from conta ct join cooperativa co on co.id = ct.cooperativa_id"
                                + " join cliente cl on cl.id = ct.cliente_id where co.codigo = ? and ct.numero = ?")) {
            ps.setString(1, coop);
            ps.setString(2, conta);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    static String texto(DataSource ds, String sql) throws SQLException {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }

    static int contar(DataSource ds, String sql) throws SQLException {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    static void executar(DataSource ds, String sql) throws SQLException {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute(sql);
        }
    }
}
