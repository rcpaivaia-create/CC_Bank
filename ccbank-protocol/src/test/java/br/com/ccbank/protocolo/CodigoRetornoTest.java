package br.com.ccbank.protocolo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class CodigoRetornoTest {

    @Test
    void catalogoCompleto() {
        assertEquals(9, CodigoRetorno.values().length);
        assertEquals("saldo insuficiente", CodigoRetorno.porCodigo("003").get().descricao());
        assertEquals(CodigoRetorno.CONTA_DESTINO_IGUAL, CodigoRetorno.porCodigo("007").get());
        assertEquals(CodigoRetorno.ERRO_INTERNO, CodigoRetorno.porCodigo("099").get());
        assertEquals(Optional.empty(), CodigoRetorno.porCodigo("123"));
    }
}
