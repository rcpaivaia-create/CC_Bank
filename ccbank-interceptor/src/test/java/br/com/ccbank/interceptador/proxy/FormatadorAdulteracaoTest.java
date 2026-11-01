package br.com.ccbank.interceptador.proxy;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.ccbank.interceptador.regras.Adulteracao;
import br.com.ccbank.protocolo.Mensagem;

class FormatadorAdulteracaoTest {

    @Test
    void campoNaoAdulteradoApareceSemMarca() {
        String s = FormatadorAdulteracao.formatar(new Mensagem().adicionar("00030", "150.00"), emptyList());
        assertTrue(s.contains("150.00"));
        assertFalse(s.contains("ADULTERADO"));
    }

    @Test
    void setMostraAntesDepoisERegra() {
        String s = FormatadorAdulteracao.formatar(new Mensagem().adicionar("00030", "1.50"),
                singletonList(new Adulteracao("00030", "150.00", "1.50", "saque-centavos")));
        assertTrue(s.contains("150.00"));
        assertTrue(s.contains("→"));
        assertTrue(s.contains("1.50"));
        assertTrue(s.contains("[ADULTERADO: regra \"saque-centavos\"]"));
    }

    @Test
    void addMostraAdicionado() {
        String s = FormatadorAdulteracao.formatar(new Mensagem().adicionar("00099", "x"),
                singletonList(new Adulteracao("00099", null, "x", "r")));
        assertTrue(s.contains("[ADICIONADO: regra \"r\"]"));
    }

    @Test
    void removeMostraLinhaRemovida() {
        String s = FormatadorAdulteracao.formatar(new Mensagem(),
                singletonList(new Adulteracao("00030", "150.00", null, "r")));
        assertTrue(s.contains("(removido)"));
        assertTrue(s.contains("[REMOVIDO: regra \"r\"]"));
    }
}
