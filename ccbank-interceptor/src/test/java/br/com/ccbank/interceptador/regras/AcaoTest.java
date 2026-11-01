package br.com.ccbank.interceptador.regras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.ccbank.protocolo.Campo;
import br.com.ccbank.protocolo.Mensagem;

class AcaoTest {

    private static List<Campo> campos(Mensagem m) {
        return new ArrayList<>(m.campos());
    }

    @Test
    void setAlteraUmaOcorrencia() {
        List<Campo> campos = campos(new Mensagem().adicionar("00030", "500.00"));
        List<Adulteracao> ad = new Acao(Acao.Tipo.SET, "00030", "5.00").aplicar(campos, "r");
        assertEquals("5.00", campos.get(0).valor());
        assertEquals(new Adulteracao("00030", "500.00", "5.00", "r"), ad.get(0));
    }

    @Test
    void setAlteraTodasAsOcorrencias() {
        List<Campo> campos = campos(new Mensagem().adicionar("00060", "a").adicionar("00060", "b"));
        List<Adulteracao> ad = new Acao(Acao.Tipo.SET, "00060", "z").aplicar(campos, "r");
        assertEquals("z", campos.get(0).valor());
        assertEquals("z", campos.get(1).valor());
        assertEquals(2, ad.size());
    }

    @Test
    void setEmRotuloAusenteEhNoOp() {
        List<Campo> campos = campos(new Mensagem().adicionar("00001", "saque"));
        assertTrue(new Acao(Acao.Tipo.SET, "00030", "5.00").aplicar(campos, "r").isEmpty());
        assertEquals(1, campos.size());
    }

    @Test
    void addAcrescentaAoFinal() {
        List<Campo> campos = campos(new Mensagem().adicionar("00001", "saque"));
        List<Adulteracao> ad = new Acao(Acao.Tipo.ADD, "00030", "9.99").aplicar(campos, "r");
        assertEquals("00030", campos.get(campos.size() - 1).rotulo());
        assertEquals(new Adulteracao("00030", null, "9.99", "r"), ad.get(0));
    }

    @Test
    void removeTiraTodasAsOcorrencias() {
        List<Campo> campos = campos(new Mensagem().adicionar("00030", "1").adicionar("00030", "2"));
        List<Adulteracao> ad = new Acao(Acao.Tipo.REMOVE, "00030", null).aplicar(campos, "r");
        assertTrue(campos.isEmpty());
        assertEquals(2, ad.size());
        assertNull(ad.get(0).depois());
    }
}
