package br.com.ccbank.testes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Gera a matriz de ataques (ataque × lado) em HTML autocontido. */
final class GeradorMatrizAtaques {

    private static final String[] LADOS = {"v1", "v2"};
    private static final DateTimeFormatter CARIMBO = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CSS =
            "body{font-family:sans-serif;margin:2rem;color:#222}"
            + "h1{font-size:1.3rem}table{border-collapse:collapse;margin-top:1rem}"
            + "th,td{border:1px solid #ccc;padding:.4rem .7rem;text-align:left}"
            + "td.vulneravel{background:#f8d7da;color:#842029;font-weight:bold}"
            + "td.protegido{background:#d1e7dd;color:#0f5132;font-weight:bold}"
            + ".meta{color:#666;font-size:.9rem}";

    private GeradorMatrizAtaques() {
    }

    static String gerar(List<Ataque> ataques, Map<String, Map<String, Veredito>> resultados, LocalDateTime geradoEm) {
        StringBuilder h = new StringBuilder();
        h.append("<!DOCTYPE html>\n<html lang=\"pt-BR\">\n<head>\n<meta charset=\"utf-8\">\n")
                .append("<title>Matriz de ataques</title>\n<style>").append(CSS).append("</style>\n</head>\n<body>\n");
        h.append("<h1>Matriz de ataques</h1>\n");
        h.append("<p class=\"meta\">Gerada em ").append(DATA_HORA.format(geradoEm)).append("</p>\n");
        h.append("<table>\n<tr><th>#</th><th>Ataque</th><th>Descrição</th><th>v1</th><th>v2</th></tr>\n");
        int n = 1;
        for (Ataque ataque : ataques) {
            h.append("<tr><td>").append(n++).append("</td><td>").append(escapar(ataque.id()))
                    .append("</td><td>").append(escapar(ataque.descricao())).append("</td>");
            Map<String, Veredito> porLado = resultados.get(ataque.id());
            for (String lado : LADOS) {
                Veredito v = porLado == null ? null : porLado.get(lado);
                h.append(celula(v));
            }
            h.append("</tr>\n");
        }
        h.append("</table>\n</body>\n</html>\n");
        return h.toString();
    }

    static Path escrever(String html) throws IOException {
        Path destino = raizRepo().resolve("resultados")
                .resolve("matriz-ataques-" + CARIMBO.format(LocalDateTime.now()) + ".html");
        Files.createDirectories(destino.getParent());
        Files.write(destino, html.getBytes(StandardCharsets.UTF_8));
        return destino;
    }

    private static String celula(Veredito v) {
        if (v == null) {
            return "<td>-</td>";
        }
        if (v == Veredito.VULNERAVEL) {
            return "<td class=\"vulneravel\">VULNERÁVEL</td>";
        }
        return "<td class=\"protegido\">PROTEGIDO</td>";
    }

    /** Raiz do repositório: o ancestral de {@code user.dir} que contém o diretório {@code regras}. */
    private static Path raizRepo() {
        Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (dir != null) {
            if (Files.isDirectory(dir.resolve("regras"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        return Paths.get(System.getProperty("user.dir")).toAbsolutePath();
    }

    private static String escapar(String texto) {
        StringBuilder sb = new StringBuilder();
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
