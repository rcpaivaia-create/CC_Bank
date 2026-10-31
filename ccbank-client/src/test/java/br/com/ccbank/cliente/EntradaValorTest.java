package br.com.ccbank.cliente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EntradaValorTest {

    @ParameterizedTest
    @CsvSource({"'1.500,00',1500.00", "'150,5',150.5", "' 10 ',10", "'150.00',150.00",
        "'1.500',1500", "'1.234.567,89',1234567.89", "'abc',abc", "'1.50',1.50", "'',''"})
    void normaliza(String digitado, String esperado) {
        assertEquals(esperado, EntradaValor.normalizar(digitado));
    }

    @Test
    void normalizaData() {
        assertEquals("2026-10-31", EntradaValor.normalizarData("31/10/2026"));
        assertEquals("2026-10-31", EntradaValor.normalizarData(" 2026-10-31 "));
        assertNull(EntradaValor.normalizarData("  "));
        assertEquals("31-10-2026", EntradaValor.normalizarData("31-10-2026"));
    }
}
