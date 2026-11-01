package br.com.ccbank.interceptador.tela;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import br.com.ccbank.protocolo.Mensagem;

class ModeloCamposEditaveisTest {

    @Test
    void montaMensagemDasLinhas() {
        ModeloCamposEditaveis m = new ModeloCamposEditaveis();
        m.popular(new Mensagem().adicionar("00001", "saque").adicionar("00030", "500.00"));
        assertEquals(2, m.getRowCount());
        m.setValueAt("0.01", 1, 2);
        assertEquals(Optional.of("0.01"), m.montarMensagem().primeiro("00030"));
        assertEquals(Optional.of("saque"), m.montarMensagem().primeiro("00001"));
    }

    @Test
    void adicionaERemoveLinha() {
        ModeloCamposEditaveis m = new ModeloCamposEditaveis();
        m.popular(new Mensagem().adicionar("00001", "saque"));
        m.adicionarLinha();
        assertEquals(2, m.getRowCount());
        m.removerLinha(1);
        assertEquals(1, m.getRowCount());
    }
}
