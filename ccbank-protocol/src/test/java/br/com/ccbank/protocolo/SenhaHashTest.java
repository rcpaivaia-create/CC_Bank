package br.com.ccbank.protocolo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SenhaHashTest {

    @Test
    void valoresConhecidos() {
        assertEquals("e10adc3949ba59abbe56e057f20f883e", SenhaHash.md5("123456"));
        assertEquals("e7d80ffeefa212b7c5c55700e4f7193e", SenhaHash.md5("senha123"));
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", SenhaHash.md5(""));
    }

    @Test
    void usaUtf8() {
        assertEquals("27c4b302cef836223fd435d6fdcf60b2", SenhaHash.md5("çãé"));
    }

    @Test
    void nuloFalha() {
        assertThrows(IllegalArgumentException.class, () -> SenhaHash.md5(null));
    }
}
