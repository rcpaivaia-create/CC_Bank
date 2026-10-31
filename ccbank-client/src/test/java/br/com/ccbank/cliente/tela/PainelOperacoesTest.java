package br.com.ccbank.cliente.tela;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import br.com.ccbank.cliente.LadoCliente;

class PainelOperacoesTest {

    @Test
    void trocaDeFormulario() {
        PainelOperacoes p = new PainelOperacoes(new PainelLado(LadoCliente.ESQUERDO));
        assertEquals("saldo", p.formularioAtual());
        p.botao("Transferência").doClick();
        assertEquals("transferencia", p.formularioAtual());
        p.botao("Extrato").doClick();
        assertEquals("extrato", p.formularioAtual());
        p.botao("Saque").doClick();
        assertEquals("saque", p.formularioAtual());
    }
}
