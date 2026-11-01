package br.com.ccbank.testes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.ccbank.interceptador.regras.Regra;
import br.com.ccbank.interceptador.regras.Sentido;

class CatalogoRegrasTest {

    @Test
    void achaOArquivoNaRaiz() {
        assertTrue(Files.exists(CatalogoRegras.arquivo()));
    }

    @Test
    void carregaUmaRegraPorNome() {
        List<Regra> r = CatalogoRegras.regra("valor-adulterado");
        assertEquals(1, r.size());
        assertEquals(Sentido.REQUISICAO, r.get(0).sentido());
    }

    @Test
    void regraInexistenteFalhaClaro() {
        assertThrows(IllegalArgumentException.class, () -> CatalogoRegras.regra("nao-existe"));
    }
}
