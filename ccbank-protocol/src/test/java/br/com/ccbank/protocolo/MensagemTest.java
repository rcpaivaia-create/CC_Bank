package br.com.ccbank.protocolo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class MensagemTest {

    @Test
    void preservaOrdemERepeticao() {
        Mensagem m = new Mensagem()
                .adicionar(Rotulo.TRANSACAO, "extrato")
                .adicionar("00081", "2026-10-31T10:00:00")
                .adicionar("00081", "2026-10-31T11:00:00");
        assertEquals(3, m.tamanho());
        assertEquals(Arrays.asList("2026-10-31T10:00:00", "2026-10-31T11:00:00"), m.todos("00081"));
        assertEquals("00001", m.campos().get(0).rotulo());
    }

    @Test
    void primeiroDevolveVazioQuandoAusente() {
        assertEquals(Optional.empty(), new Mensagem().primeiro(Rotulo.VALOR));
    }

    @Test
    void aceitaRotuloDesconhecidoEValorComIgualEVazio() {
        Mensagem m = new Mensagem().adicionar("54321", "a=b").adicionar("00030", "");
        assertEquals(Optional.of("a=b"), m.primeiro("54321"));
        assertEquals(Optional.of(""), m.primeiro("00030"));
    }

    @Test
    void rejeitaCampoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new Campo("0001", "x"));
        assertThrows(IllegalArgumentException.class, () -> new Campo("0000A", "x"));
        assertThrows(IllegalArgumentException.class, () -> new Campo("00001", "a\nb"));
        assertThrows(IllegalArgumentException.class, () -> new Campo("00001", "a\rb"));
        assertThrows(IllegalArgumentException.class, () -> new Campo("00001", null));
    }

    @Test
    void camposNaoModificavel() {
        Mensagem m = new Mensagem().adicionar("00001", "saldo");
        assertThrows(UnsupportedOperationException.class, () -> m.campos().clear());
    }

    @Test
    void igualdadePorConteudo() {
        assertEquals(new Mensagem().adicionar("00001", "saldo"), new Mensagem().adicionar("00001", "saldo"));
    }
}
