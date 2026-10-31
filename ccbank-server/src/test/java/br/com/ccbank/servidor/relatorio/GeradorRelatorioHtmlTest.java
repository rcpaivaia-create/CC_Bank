package br.com.ccbank.servidor.relatorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class GeradorRelatorioHtmlTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 31, 14, 3, 22);

    private static FiltroRelatorio filtroHoje() {
        return new FiltroRelatorio(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 31));
    }

    /** Builder local de {@link LinhaRelatorio} com valores padrão. */
    private static final class Linha {
        private String tipo = "SAQUE";
        private String operador = "rodrigop";
        private BigDecimal valor = new BigDecimal("10.00");
        private String codigo = "000";
        private boolean divergente;
        private String terminalCadastrado = "0001/001/001";

        Linha tipo(String v) {
            tipo = v;
            return this;
        }

        Linha operador(String v) {
            operador = v;
            return this;
        }

        Linha valor(String v) {
            valor = v == null ? null : new BigDecimal(v);
            return this;
        }

        Linha codigo(String v) {
            codigo = v;
            return this;
        }

        Linha divergente(boolean v) {
            divergente = v;
            return this;
        }

        Linha terminalCadastrado(String v) {
            terminalCadastrado = v;
            return this;
        }

        LinhaRelatorio criar() {
            return new LinhaRelatorio(1, AGORA, tipo, operador, "0001", "001", "001", valor,
                    "0001/100001", "", codigo, false, divergente, terminalCadastrado);
        }
    }

    private static Linha linha() {
        return new Linha();
    }

    private static List<LinhaRelatorio> lista(Linha... linhas) {
        LinhaRelatorio[] criadas = new LinhaRelatorio[linhas.length];
        for (int i = 0; i < linhas.length; i++) {
            criadas[i] = linhas[i].criar();
        }
        return Arrays.asList(criadas);
    }

    @Test
    void escapaConteudoDoJornal() {
        String html = GeradorRelatorioHtml.gerar("v1", filtroHoje(), lista(linha().operador("<script>alert(1)</script>")), AGORA);
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
    }

    @Test
    void divergenteDestacadaComTerminalCadastrado() {
        String html = GeradorRelatorioHtml.gerar("v1", filtroHoje(),
                lista(linha().divergente(true).terminalCadastrado("0001/001/001")), AGORA);
        assertTrue(html.contains("class=\"divergente\""));
        assertTrue(html.contains("title=\"Terminal cadastrado: 0001/001/001\""));
    }

    @Test
    void resumoContaApenasEfetivadasNaSoma() {
        String html = GeradorRelatorioHtml.gerar("v1", filtroHoje(), lista(
                linha().valor("10.00"),
                linha().valor("5.50"),
                linha().valor("100.00").codigo("003"),
                linha().tipo("SALDO").valor(null)), AGORA);
        assertTrue(html.contains("<td>SAQUE</td><td>3</td><td>15.50</td>"));
        assertTrue(html.contains("<td>SALDO</td><td>1</td><td>0.00</td>"));
        assertTrue(html.contains("Recusadas: 1"));
        assertTrue(html.contains("class=\"recusada\""));
        assertTrue(html.contains("003 – saldo insuficiente"));
    }

    @Test
    void diaSemMovimento() {
        String html = GeradorRelatorioHtml.gerar("v2", filtroHoje(), Collections.emptyList(), AGORA);
        assertTrue(html.startsWith("<!DOCTYPE html>"));
        assertTrue(html.contains("Nenhuma operação encontrada"));
        assertTrue(html.contains("Divergências de origem: 0"));
        assertTrue(html.contains("Relatório de operações · v2"));
    }

    @Test
    void semRecursosExternos() {
        String html = GeradorRelatorioHtml.gerar("v1", filtroHoje(), lista(linha()), AGORA);
        assertFalse(html.contains("src="));
        assertFalse(html.contains("href="));
    }

    @Test
    void escapar() {
        assertEquals("&amp;&lt;&gt;&quot;&#39;", GeradorRelatorioHtml.escapar("&<>\"'"));
        assertEquals("", GeradorRelatorioHtml.escapar(null));
    }
}
