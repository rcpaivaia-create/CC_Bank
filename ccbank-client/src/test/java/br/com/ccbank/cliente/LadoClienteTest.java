package br.com.ccbank.cliente;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LadoClienteTest {

    @Test
    void portas() {
        assertEquals(9001, LadoCliente.ESQUERDO.porta(false));
        assertEquals(8001, LadoCliente.ESQUERDO.porta(true));
        assertEquals(9002, LadoCliente.DIREITO.porta(false));
        assertEquals(8002, LadoCliente.DIREITO.porta(true));
        assertEquals("DIR", LadoCliente.DIREITO.sigla());
    }

    @Test
    void hostPadrao() {
        assertEquals("127.0.0.1", LadoCliente.host());
    }
}
