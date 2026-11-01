package br.com.ccbank.testes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class GeradorMatrizAtaquesTest {

    @Test
    void matrizTemCelulasEVereditos() {
        Map<String, Veredito> porLado = new HashMap<>();
        porLado.put("v1", Veredito.VULNERAVEL);
        porLado.put("v2", Veredito.PROTEGIDO);
        Map<String, Map<String, Veredito>> resultados = new HashMap<>();
        resultados.put("valor-adulterado", porLado);

        String html = GeradorMatrizAtaques.gerar(CatalogoAtaques.todos(), resultados, LocalDateTime.now());

        assertTrue(html.startsWith("<!DOCTYPE html>"));
        assertTrue(html.contains("valor-adulterado"));
        assertTrue(html.contains("VULNERÁVEL"));
        assertTrue(html.contains("PROTEGIDO"));
    }
}
