package br.com.ccbank.protocolo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

class RotuloTest {

    @Test
    void catalogoTem26Rotulos() {
        assertEquals(26, Rotulo.values().length);
    }

    @Test
    void codigosSaoCincoDigitosEUnicos() {
        Set<String> vistos = new HashSet<>();
        for (Rotulo r : Rotulo.values()) {
            assertTrue(r.codigo().matches("\\d{5}"), r.name());
            assertTrue(vistos.add(r.codigo()), "duplicado: " + r.codigo());
        }
    }

    @Test
    void buscaPorCodigo() {
        assertEquals(Optional.of(Rotulo.TRANSACAO), Rotulo.porCodigo("00001"));
        assertEquals("cooperativa_operador", Rotulo.porCodigo("00004").get().nome());
        assertEquals("lanc_contraparte", Rotulo.porCodigo("00085").get().nome());
        assertEquals(Optional.empty(), Rotulo.porCodigo("99999"));
        assertEquals(Optional.empty(), Rotulo.porCodigo(null));
    }
}
