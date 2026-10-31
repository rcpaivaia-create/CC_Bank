package br.com.ccbank.servidor.relatorio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArquivoRelatorioTest {

    @Test
    void nomeDoArquivo() {
        assertEquals("relatorio-v2-2026-10-31-140322.html",
                ArquivoRelatorio.nome("v2", LocalDateTime.of(2026, 10, 31, 14, 3, 22)));
    }

    @Test
    void salvaEmUtf8CriandoPasta(@TempDir Path dir) throws IOException {
        Path f = ArquivoRelatorio.salvar(dir.resolve("relatorios"), "r.html", "<p>ação</p>");
        assertEquals("<p>ação</p>", new String(Files.readAllBytes(f), StandardCharsets.UTF_8));
    }
}
