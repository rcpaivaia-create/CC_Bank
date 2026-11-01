package br.com.ccbank.interceptador.tela;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import br.com.ccbank.interceptador.historico.Historico;
import br.com.ccbank.interceptador.mediacao.DecisaoMediacao;
import br.com.ccbank.interceptador.regras.Sentido;
import br.com.ccbank.protocolo.Mensagem;

class ModeloHistoricoTest {

    @Test
    void refleteOHistorico() {
        Historico h = new Historico();
        Mensagem orig = new Mensagem().adicionar("00001", "saque");
        h.observar(Sentido.REQUISICAO, orig, DecisaoMediacao.encaminharOriginal(orig));
        ModeloHistorico mh = new ModeloHistorico(h);
        mh.atualizar();
        assertEquals(1, mh.getRowCount());
        assertEquals("saque", mh.linha(0).transacao());
    }
}
