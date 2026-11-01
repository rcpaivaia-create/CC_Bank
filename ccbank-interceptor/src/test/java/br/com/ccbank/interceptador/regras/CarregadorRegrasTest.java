package br.com.ccbank.interceptador.regras;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

class CarregadorRegrasTest {

    private static ByteArrayInputStream json(String texto) {
        return new ByteArrayInputStream(texto.getBytes(UTF_8));
    }

    @Test
    void carregaNaOrdemComCamposCertos() throws Exception {
        List<Regra> regras = CarregadorRegras.carregar(getClass().getResourceAsStream("/regras-validas.json"));
        assertEquals(2, regras.size());
        assertEquals("saque-centavos", regras.get(0).nome());
        assertEquals(Sentido.REQUISICAO, regras.get(0).sentido());
        assertEquals(Acao.Tipo.SET, regras.get(0).acoes().get(0).tipo());
        assertEquals("00030", regras.get(0).acoes().get(0).rotulo());
        assertEquals("0.01", regras.get(0).acoes().get(0).valor());
    }

    @Test
    void ativaAusenteAssumeTrue() throws Exception {
        List<Regra> regras = CarregadorRegras.carregar(getClass().getResourceAsStream("/regras-validas.json"));
        assertTrue(regras.get(1).ativa());
    }

    @Test
    void jsonMalformadoLancaRegrasInvalidas() {
        assertThrows(RegrasInvalidasException.class, () -> CarregadorRegras.carregar(json("{nao é json")));
    }

    @Test
    void tipoDeAcaoInvalidoLancaRegrasInvalidas() {
        assertThrows(RegrasInvalidasException.class,
                () -> CarregadorRegras.carregar(getClass().getResourceAsStream("/regras-tipo-invalido.json")));
    }

    @Test
    void direcaoInvalidaLancaRegrasInvalidas() {
        String texto = "[{\"nome\":\"x\",\"direcao\":\"XPTO\",\"quando\":{\"00001\":\"saque\"},"
                + "\"acoes\":[{\"tipo\":\"SET\",\"rotulo\":\"00030\",\"valor\":\"0\"}]}]";
        assertThrows(RegrasInvalidasException.class, () -> CarregadorRegras.carregar(json(texto)));
    }

    @Test
    void setSemValorLancaRegrasInvalidas() {
        String texto = "[{\"nome\":\"x\",\"direcao\":\"REQUISICAO\",\"quando\":{\"00001\":\"saque\"},"
                + "\"acoes\":[{\"tipo\":\"SET\",\"rotulo\":\"00030\"}]}]";
        assertThrows(RegrasInvalidasException.class, () -> CarregadorRegras.carregar(json(texto)));
    }

    @Test
    void regexInvalidaNoQuandoLancaRegrasInvalidas() {
        String texto = "[{\"nome\":\"x\",\"direcao\":\"REQUISICAO\",\"quando\":{\"00030\":\"~(\"},"
                + "\"acoes\":[{\"tipo\":\"SET\",\"rotulo\":\"00030\",\"valor\":\"0\"}]}]";
        assertThrows(RegrasInvalidasException.class, () -> CarregadorRegras.carregar(json(texto)));
    }
}
