package br.com.ccbank.interceptador.mediacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import br.com.ccbank.interceptador.regras.Sentido;
import br.com.ccbank.protocolo.Mensagem;

class MediadorPassagemTest {

    @Test
    void encaminhaOriginalSemReserializar() {
        DecisaoMediacao d = new MediadorPassagem().intervir(
                new Mensagem().adicionar("00001", "saque"), Sentido.REQUISICAO);
        assertEquals(DecisaoMediacao.Acao.ENCAMINHAR, d.acao());
        assertFalse(d.reserializar());
    }
}
