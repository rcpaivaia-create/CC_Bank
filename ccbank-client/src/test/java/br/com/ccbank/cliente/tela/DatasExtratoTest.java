package br.com.ccbank.cliente.tela;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class DatasExtratoTest {

    @Test
    void inicioEh30DiasAntes() {
        assertEquals("02/10/2026", DatasExtrato.inicio(LocalDate.of(2026, 11, 1)));
    }

    @Test
    void fimEhODia() {
        assertEquals("01/11/2026", DatasExtrato.fim(LocalDate.of(2026, 11, 1)));
    }
}
