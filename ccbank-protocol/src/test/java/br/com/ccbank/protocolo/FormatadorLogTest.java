package br.com.ccbank.protocolo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FormatadorLogTest {

    @Test
    void formataCamposComColunaAlinhada() {
        Mensagem m = new Mensagem().adicionar("00001", "saque").adicionar("00004", "0001").adicionar("77777", "x");
        assertEquals(
                "    00001 transacao ........... saque\n"
                        + "    00004 cooperativa_operador  0001\n"
                        + "    77777 desconhecido ........ x",
                FormatadorLog.formatarCampos(m));
    }

    @Test
    void codigoDeRetornoGanhaDescricao() {
        assertEquals("    00090 codigo_retorno ...... 000 (sucesso)",
                FormatadorLog.formatarCampos(new Mensagem().adicionar("00090", "000")));
        assertEquals("    00090 codigo_retorno ...... 555",
                FormatadorLog.formatarCampos(new Mensagem().adicionar("00090", "555")));
    }

    @Test
    void cabecalhoEnviadaSemTempo() {
        Mensagem m = new Mensagem().adicionar("00001", "saldo");
        assertEquals("[ESQ] ENVIADA → 127.0.0.1:9001 (1 campos)\n    00001 transacao ........... saldo",
                FormatadorLog.formatarTroca("ESQ", Direcao.ENVIADA, "127.0.0.1:9001", m, null));
    }

    @Test
    void cabecalhoRecebidaComTempo() {
        Mensagem m = new Mensagem().adicionar("00090", "003");
        assertTrue(FormatadorLog.formatarTroca("ESQ", Direcao.RECEBIDA, "127.0.0.1:9001", m, 46L)
                .startsWith("[ESQ] RECEBIDA ← 127.0.0.1:9001 (1 campos) em 46 ms\n"));
    }
}
