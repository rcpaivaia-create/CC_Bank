package br.com.ccbank.testes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AtaquesOfflineTest {

    @Test
    void quebraHashRecuperaSenha() throws Exception {
        assertEquals(Veredito.VULNERAVEL, new QuebraHash().avaliar(null));
    }

    @Test
    void chaveAesDecifraOArquivo() throws Exception {
        assertEquals(Veredito.VULNERAVEL, new ChaveAesNoCodigo().avaliar(null));
    }
}
