package br.com.ccbank.cliente;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.GeneralSecurityException;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class CifraArquivoTest {

    @Test
    void idaEVolta() throws Exception {
        assertEquals("rodrigop", CifraArquivo.decifrar(CifraArquivo.cifrar("rodrigop")));
    }

    @Test
    void acentos() throws Exception {
        assertEquals("joão", CifraArquivo.decifrar(CifraArquivo.cifrar("joão")));
    }

    @Test
    void ivDiferenteACadaCifragem() {
        assertNotEquals(CifraArquivo.cifrar("x"), CifraArquivo.cifrar("x"));
    }

    @Test
    void naoContemOTextoEmClaro() {
        byte[] bytes = Base64.getDecoder().decode(CifraArquivo.cifrar("rodrigop"));
        assertFalse(new String(bytes, ISO_8859_1).contains("rodrigop"));
        assertEquals(32, bytes.length); // IV de 16 bytes + 1 bloco
    }

    @Test
    void conteudoAdulteradoFalha() {
        assertThrows(GeneralSecurityException.class, () -> CifraArquivo.decifrar("não é base64!"));
        assertThrows(GeneralSecurityException.class,
                () -> CifraArquivo.decifrar(Base64.getEncoder().encodeToString(new byte[10])));
        // truncado: o conteúdo cifrado deixa de ser múltiplo do bloco (erro determinístico)
        byte[] bytes = Base64.getDecoder().decode(CifraArquivo.cifrar("rodrigop"));
        String truncado = Base64.getEncoder().encodeToString(java.util.Arrays.copyOf(bytes, bytes.length - 1));
        assertThrows(GeneralSecurityException.class, () -> CifraArquivo.decifrar(truncado));
    }
}
