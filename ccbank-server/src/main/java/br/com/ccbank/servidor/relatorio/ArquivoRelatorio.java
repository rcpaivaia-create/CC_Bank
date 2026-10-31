package br.com.ccbank.servidor.relatorio;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Nome e gravação dos arquivos de relatório. */
public final class ArquivoRelatorio {

    private static final DateTimeFormatter CARIMBO = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss");

    private ArquivoRelatorio() {
    }

    /** {@code relatorio-<ambiente>-<AAAA-MM-DD-HHMMSS>.html} */
    public static String nome(String ambiente, LocalDateTime quando) {
        return "relatorio-" + ambiente + "-" + CARIMBO.format(quando) + ".html";
    }

    public static Path salvar(Path pasta, String nome, String html) throws IOException {
        Files.createDirectories(pasta);
        Path arquivo = pasta.resolve(nome);
        Files.write(arquivo, html.getBytes(StandardCharsets.UTF_8));
        return arquivo;
    }
}
