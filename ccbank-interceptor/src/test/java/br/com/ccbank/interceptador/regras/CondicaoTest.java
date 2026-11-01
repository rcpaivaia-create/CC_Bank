package br.com.ccbank.interceptador.regras;

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import br.com.ccbank.protocolo.Mensagem;

class CondicaoTest {

    @Test
    void literalCasa() {
        Mensagem m = new Mensagem().adicionar("00001", "saque");
        assertTrue(new Condicao(singletonMap("00001", "saque")).casa(m));
    }

    @Test
    void literalNaoCasa() {
        Mensagem m = new Mensagem().adicionar("00001", "deposito");
        assertFalse(new Condicao(singletonMap("00001", "saque")).casa(m));
    }

    @Test
    void regexCasaValorInteiro() {
        Mensagem m = new Mensagem().adicionar("00030", "1500.00");
        assertTrue(new Condicao(singletonMap("00030", "~[0-9.]+")).casa(m));
    }

    @Test
    void regexNaoCasaParcial() {
        Mensagem m = new Mensagem().adicionar("00020", "100001");
        assertFalse(new Condicao(singletonMap("00020", "~1000")).casa(m));
    }

    @Test
    void todosOsRotulosPrecisamCasar() {
        Map<String, String> padroes = new LinkedHashMap<>();
        padroes.put("00001", "saque");
        padroes.put("00030", "500.00");
        Mensagem m = new Mensagem().adicionar("00001", "saque").adicionar("00030", "10.00");
        assertFalse(new Condicao(padroes).casa(m));
    }

    @Test
    void rotuloAusenteNaoCasa() {
        assertFalse(new Condicao(singletonMap("00001", "saque")).casa(new Mensagem()));
    }

    @Test
    void regexInvalidaLancaNaConstrucao() {
        assertThrows(IllegalArgumentException.class, () -> new Condicao(singletonMap("00030", "~(")));
    }
}
