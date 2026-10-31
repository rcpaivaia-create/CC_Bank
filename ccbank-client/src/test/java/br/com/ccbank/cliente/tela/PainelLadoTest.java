package br.com.ccbank.cliente.tela;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.ccbank.cliente.LadoCliente;

class PainelLadoTest {

    @Test
    void estadoInicial() {
        PainelLado p = new PainelLado(LadoCliente.ESQUERDO);
        assertFalse(p.logado());
        assertEquals("login", p.cartaoAtual());
        assertTrue(p.checkInterceptador().isEnabled());
        assertFalse(p.checkInterceptador().isSelected());
        assertEquals("127.0.0.1:9001", p.cliente().endereco());
    }

    @Test
    void checkInterceptadorTrocaPorta() {
        PainelLado p = new PainelLado(LadoCliente.DIREITO);
        p.checkInterceptador().doClick();
        assertEquals("127.0.0.1:8002", p.cliente().endereco());
        p.checkInterceptador().doClick();
        assertEquals("127.0.0.1:9002", p.cliente().endereco());
    }
}
