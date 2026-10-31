package br.com.ccbank.protocolo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FormatoValoresTest {

    @Test
    void valoresValidos() {
        assertEquals(new BigDecimal("150.00"), FormatoValores.parseValorOperacao("150.00").get());
        assertEquals(new BigDecimal("150.00"), FormatoValores.parseValorOperacao("150").get());
        assertEquals(new BigDecimal("0.50"), FormatoValores.parseValorOperacao("0.5").get());
        assertEquals(new BigDecimal("9999999999999.99"), FormatoValores.parseValorOperacao("9999999999999.99").get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0", "0.00", "-10.00", "+10.00", "150,00", "1.500,00", "10.001", "abc",
            " 10.00", "10.", ".50", "10000000000000.00"})
    void valoresInvalidos(String texto) {
        assertEquals(Optional.empty(), FormatoValores.parseValorOperacao(texto));
    }

    @Test
    void valorNulo() {
        assertEquals(Optional.empty(), FormatoValores.parseValorOperacao(null));
    }

    @Test
    void formataValor() {
        assertEquals("150.00", FormatoValores.formatarValor(new BigDecimal("150")));
        assertEquals("-150.50", FormatoValores.formatarValor(new BigDecimal("-150.5")));
        assertEquals("0.00", FormatoValores.formatarValor(BigDecimal.ZERO));
    }

    @Test
    void datas() {
        assertEquals(LocalDate.of(2026, 10, 31), FormatoValores.parseData("2026-10-31").get());
        assertEquals(Optional.empty(), FormatoValores.parseData("31/10/2026"));
        assertEquals(Optional.empty(), FormatoValores.parseData("2026-02-30"));
        assertEquals(Optional.empty(), FormatoValores.parseData(null));
        assertEquals("2026-10-31", FormatoValores.formatarData(LocalDate.of(2026, 10, 31)));
        assertEquals("2026-10-31T14:03:22",
                FormatoValores.formatarDataHora(LocalDateTime.of(2026, 10, 31, 14, 3, 22, 123_000_000)));
    }
}
