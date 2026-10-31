package br.com.ccbank.servidor.relatorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

/**
 * Lê o jornal para o relatório. A divergência de origem compara cooperativa, PAC e terminal
 * alegados na mensagem com o terminal cadastrado do operador (não se aplica a logins).
 */
public final class ConsultaRelatorio {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private static final String BASE =
            "with base as ("
                    + " select t.id, t.data_hora, t.tipo, t.operador_login, t.cooperativa, t.pac, t.terminal, t.valor,"
                    + "  t.cooperativa_origem, t.conta_origem, t.cooperativa_destino, t.conta_destino,"
                    + "  t.codigo_retorno, t.intercredis,"
                    + "  o.id is not null as operador_existe, co.codigo as coop_cad, p.codigo as pac_cad, te.codigo as term_cad,"
                    + "  (t.tipo <> 'LOGIN' and (o.id is null"
                    + "    or t.cooperativa is distinct from co.codigo"
                    + "    or t.pac is distinct from p.codigo"
                    + "    or t.terminal is distinct from te.codigo)) as divergente"
                    + " from transacao t"
                    + " left join operador o on o.login = t.operador_login"
                    + " left join terminal te on te.id = o.terminal_id"
                    + " left join pac p on p.id = te.pac_id"
                    + " left join cooperativa co on co.id = p.cooperativa_id"
                    + " where t.data_hora >= ?::date and t.data_hora < ?::date + 1"
                    + ") select * from base where true";

    private final DataSource ds;

    public ConsultaRelatorio(DataSource ds) {
        this.ds = ds;
    }

    public List<LinhaRelatorio> consultar(FiltroRelatorio f) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE);
        List<Object> parametros = new ArrayList<>();
        parametros.add(f.getInicio());
        parametros.add(f.getFim());
        filtroTexto(sql, parametros, "cooperativa", f.getCooperativa());
        filtroTexto(sql, parametros, "pac", f.getPac());
        filtroTexto(sql, parametros, "terminal", f.getTerminal());
        filtroTexto(sql, parametros, "operador_login", f.getOperador());
        filtroTexto(sql, parametros, "tipo", f.getTipo());
        if (f.isSomenteIntercredis()) {
            sql.append(" and intercredis");
        }
        if (f.isSomenteDivergencias()) {
            sql.append(" and divergente");
        }
        if (!f.isIncluirRecusadas()) {
            sql.append(" and codigo_retorno = '000'");
        }
        sql.append(" order by data_hora, id");

        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            List<LinhaRelatorio> linhas = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    linhas.add(ler(rs));
                }
            }
            return linhas;
        }
    }

    private static void filtroTexto(StringBuilder sql, List<Object> parametros, String coluna, String valor) {
        if (valor != null && !valor.trim().isEmpty()) {
            sql.append(" and ").append(coluna).append(" = ?");
            parametros.add(valor.trim());
        }
    }

    private static LinhaRelatorio ler(ResultSet rs) throws SQLException {
        String tipo = rs.getString("tipo");
        String terminalCadastrado;
        if ("LOGIN".equals(tipo)) {
            terminalCadastrado = "";
        } else if (!rs.getBoolean("operador_existe")) {
            terminalCadastrado = "operador inexistente";
        } else {
            terminalCadastrado = rs.getString("coop_cad") + "/" + rs.getString("pac_cad") + "/" + rs.getString("term_cad");
        }
        return new LinhaRelatorio(
                rs.getLong("id"),
                rs.getObject("data_hora", OffsetDateTime.class).atZoneSameInstant(FUSO).toLocalDateTime(),
                tipo,
                rs.getString("operador_login"),
                rs.getString("cooperativa"),
                rs.getString("pac"),
                rs.getString("terminal"),
                rs.getBigDecimal("valor"),
                conta(rs.getString("cooperativa_origem"), rs.getString("conta_origem")),
                conta(rs.getString("cooperativa_destino"), rs.getString("conta_destino")),
                rs.getString("codigo_retorno"),
                rs.getBoolean("intercredis"),
                rs.getBoolean("divergente"),
                terminalCadastrado);
    }

    private static String conta(String cooperativa, String numero) {
        if (cooperativa == null && numero == null) {
            return "";
        }
        return (cooperativa == null ? "" : cooperativa) + "/" + (numero == null ? "" : numero);
    }
}
