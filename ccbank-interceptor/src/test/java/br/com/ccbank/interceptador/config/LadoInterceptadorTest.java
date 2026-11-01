package br.com.ccbank.interceptador.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LadoInterceptadorTest {

    @Test
    void portasDoEsquerdo() {
        assertEquals(8001, LadoInterceptador.ESQUERDO.portaEscuta());
        assertEquals(9001, LadoInterceptador.ESQUERDO.portaDestino());
        assertEquals("127.0.0.1", LadoInterceptador.ESQUERDO.hostDestino());
    }

    @Test
    void portasDoDireito() {
        assertEquals(8002, LadoInterceptador.DIREITO.portaEscuta());
        assertEquals(9002, LadoInterceptador.DIREITO.portaDestino());
    }

    @Test
    void porNomeIgnoraCaixa() {
        assertEquals(LadoInterceptador.ESQUERDO, LadoInterceptador.porNome("Esquerdo"));
    }

    @Test
    void porNomeInvalidoLanca() {
        assertThrows(IllegalArgumentException.class, () -> LadoInterceptador.porNome("meio"));
    }
}
