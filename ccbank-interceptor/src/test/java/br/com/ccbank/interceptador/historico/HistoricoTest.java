package br.com.ccbank.interceptador.historico;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.ccbank.interceptador.mediacao.DecisaoMediacao;
import br.com.ccbank.interceptador.regras.ResultadoAplicacao;
import br.com.ccbank.interceptador.regras.Sentido;
import br.com.ccbank.protocolo.Mensagem;

class HistoricoTest {

    @Test
    void registraComTransacaoEAdulteracao() {
        Historico h = new Historico();
        Mensagem orig = new Mensagem().adicionar("00001", "saque").adicionar("00030", "500.00");
        h.observar(Sentido.REQUISICAO, orig, DecisaoMediacao.encaminharModificada(
                new ResultadoAplicacao(new Mensagem().adicionar("00001", "saque").adicionar("00030", "0.01"), emptyList())));
        assertEquals(1, h.trocas().size());
        assertEquals("saque", h.trocas().get(0).transacao());
        assertTrue(h.trocas().get(0).adulterada());
        assertEquals(Sentido.REQUISICAO, h.trocas().get(0).sentido());
    }

    @Test
    void passagemNaoEhAdulterada() {
        Historico h = new Historico();
        Mensagem orig = new Mensagem().adicionar("00001", "saldo");
        h.observar(Sentido.REQUISICAO, orig, DecisaoMediacao.encaminharOriginal(orig));
        assertFalse(h.trocas().get(0).adulterada());
    }

    @Test
    void descarteRegistraAcaoDescartar() {
        Historico h = new Historico();
        Mensagem orig = new Mensagem().adicionar("00001", "saque");
        h.observar(Sentido.REQUISICAO, orig, DecisaoMediacao.descartar());
        assertEquals(DecisaoMediacao.Acao.DESCARTAR, h.trocas().get(0).acao());
        assertEquals(orig, h.trocas().get(0).resultante());
    }
}
