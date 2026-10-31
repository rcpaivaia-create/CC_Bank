package br.com.ccbank.servidor.relatorio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import br.com.ccbank.protocolo.CodigoRetorno;
import br.com.ccbank.protocolo.FormatoValores;

/**
 * Gera o relatório de operações como um HTML autocontido (CSS embutido, sem recursos externos).
 * Todo texto vindo do jornal é escapado: os campos podem ter sido adulterados.
 */
public final class GeradorRelatorioHtml {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final String CSS =
            "body{font-family:Segoe UI,Arial,sans-serif;margin:24px;color:#222;background:#fff}"
                    + "h1{font-size:20px;margin:0 0 4px}"
                    + ".meta{color:#555;font-size:13px;margin:2px 0}"
                    + "table{border-collapse:collapse;margin:12px 0;font-size:12px}"
                    + "th,td{border:1px solid #ccc;padding:4px 8px;text-align:left;white-space:nowrap}"
                    + "th{background:#f0f0f0}"
                    + "td.num{text-align:right}"
                    + "tr.recusada td{color:#8a1c1c}"
                    + "tr.divergente td{background:#fff3cd;font-weight:600}"
                    + ".totais{display:flex;gap:24px;font-size:14px;margin:8px 0}"
                    + ".vazio{color:#555;font-style:italic}";

    private GeradorRelatorioHtml() {
    }

    public static String gerar(String ambiente, FiltroRelatorio filtro, List<LinhaRelatorio> linhas,
            LocalDateTime geradoEm) {
        StringBuilder h = new StringBuilder(4096 + linhas.size() * 400);
        h.append("<!DOCTYPE html>\n<html lang=\"pt-BR\">\n<head>\n<meta charset=\"utf-8\">\n")
                .append("<title>Relatório de operações · ").append(escapar(ambiente)).append("</title>\n")
                .append("<style>").append(CSS).append("</style>\n</head>\n<body>\n");

        h.append("<h1>Relatório de operações · ").append(escapar(ambiente)).append("</h1>\n");
        h.append("<p class=\"meta\">Período: ").append(DATA.format(filtro.getInicio()))
                .append(" a ").append(DATA.format(filtro.getFim())).append("</p>\n");
        h.append("<p class=\"meta\">Filtros: ").append(escapar(descreverFiltros(filtro))).append("</p>\n");
        h.append("<p class=\"meta\">Gerado em ").append(DATA_HORA.format(geradoEm)).append("</p>\n");

        resumo(h, linhas);
        detalhe(h, linhas);
        h.append("</body>\n</html>\n");
        return h.toString();
    }

    public static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(texto.length());
        for (int i = 0; i < texto.length(); i++) {
            char ch = texto.charAt(i);
            switch (ch) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#39;");
                    break;
                default:
                    sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static void resumo(StringBuilder h, List<LinhaRelatorio> linhas) {
        Map<String, int[]> quantidades = new TreeMap<>();
        Map<String, BigDecimal> somas = new TreeMap<>();
        int recusadas = 0;
        int intercredis = 0;
        int divergencias = 0;
        for (LinhaRelatorio l : linhas) {
            String tipo = l.tipo() == null ? "" : l.tipo();
            quantidades.computeIfAbsent(tipo, t -> new int[1])[0]++;
            somas.putIfAbsent(tipo, BigDecimal.ZERO);
            boolean efetivada = CodigoRetorno.SUCESSO.codigo().equals(l.codigoRetorno());
            if (efetivada && l.valor() != null) {
                somas.put(tipo, somas.get(tipo).add(l.valor()));
            }
            if (!efetivada) {
                recusadas++;
            }
            if (l.intercredis()) {
                intercredis++;
            }
            if (l.divergente()) {
                divergencias++;
            }
        }
        h.append("<h2>Resumo</h2>\n<table>\n<tr><th>Operação</th><th>Quantidade</th><th>Soma efetivada</th></tr>\n");
        for (Map.Entry<String, int[]> e : quantidades.entrySet()) {
            h.append("<tr><td>").append(escapar(e.getKey())).append("</td><td>").append(e.getValue()[0])
                    .append("</td><td>").append(FormatoValores.formatarValor(somas.get(e.getKey())))
                    .append("</td></tr>\n");
        }
        h.append("</table>\n<div class=\"totais\">")
                .append("<span>Operações: ").append(linhas.size()).append("</span>")
                .append("<span>Recusadas: ").append(recusadas).append("</span>")
                .append("<span>Intercredis: ").append(intercredis).append("</span>")
                .append("<span>Divergências de origem: ").append(divergencias).append("</span>")
                .append("</div>\n");
    }

    private static void detalhe(StringBuilder h, List<LinhaRelatorio> linhas) {
        h.append("<h2>Operações</h2>\n");
        if (linhas.isEmpty()) {
            h.append("<p class=\"vazio\">Nenhuma operação encontrada</p>\n");
            return;
        }
        h.append("<table>\n<tr><th>Data/hora</th><th>Cooperativa</th><th>PAC</th><th>Terminal</th>"
                + "<th>Operador</th><th>Operação</th><th>Valor</th><th>Conta origem</th><th>Conta destino</th>"
                + "<th>Retorno</th><th>Intercredis</th><th>Divergência</th></tr>\n");
        for (LinhaRelatorio l : linhas) {
            boolean recusada = !CodigoRetorno.SUCESSO.codigo().equals(l.codigoRetorno());
            List<String> classes = new ArrayList<>();
            if (l.divergente()) {
                classes.add("divergente");
            }
            if (recusada) {
                classes.add("recusada");
            }
            h.append("<tr");
            if (!classes.isEmpty()) {
                h.append(" class=\"").append(String.join(" ", classes)).append('"');
            }
            if (l.divergente()) {
                h.append(" title=\"Terminal cadastrado: ").append(escapar(l.terminalCadastrado())).append('"');
            }
            h.append('>');
            celula(h, DATA_HORA.format(l.dataHora()));
            celula(h, l.cooperativa());
            celula(h, l.pac());
            celula(h, l.terminal());
            celula(h, l.operador());
            celula(h, l.tipo());
            h.append("<td class=\"num\">")
                    .append(l.valor() == null ? "" : FormatoValores.formatarValor(l.valor())).append("</td>");
            celula(h, l.contaOrigem());
            celula(h, l.contaDestino());
            celula(h, retorno(l.codigoRetorno()));
            celula(h, l.intercredis() ? "Sim" : "");
            celula(h, l.divergente() ? "Sim" : "");
            h.append("</tr>\n");
        }
        h.append("</table>\n");
    }

    private static void celula(StringBuilder h, String texto) {
        h.append("<td>").append(escapar(texto)).append("</td>");
    }

    private static String retorno(String codigo) {
        Optional<CodigoRetorno> c = CodigoRetorno.porCodigo(codigo);
        return c.isPresent() ? codigo + " – " + c.get().descricao() : codigo;
    }

    private static String descreverFiltros(FiltroRelatorio f) {
        List<String> partes = new ArrayList<>();
        acrescentar(partes, "cooperativa", f.getCooperativa());
        acrescentar(partes, "PAC", f.getPac());
        acrescentar(partes, "terminal", f.getTerminal());
        acrescentar(partes, "operador", f.getOperador());
        acrescentar(partes, "operação", f.getTipo());
        if (f.isSomenteIntercredis()) {
            partes.add("só Intercredis");
        }
        if (f.isSomenteDivergencias()) {
            partes.add("só divergências de origem");
        }
        partes.add(f.isIncluirRecusadas() ? "inclui recusadas" : "sem recusadas");
        return String.join(", ", partes);
    }

    private static void acrescentar(List<String> partes, String nome, String valor) {
        if (valor != null && !valor.trim().isEmpty()) {
            partes.add(nome + " " + valor.trim());
        }
    }
}
