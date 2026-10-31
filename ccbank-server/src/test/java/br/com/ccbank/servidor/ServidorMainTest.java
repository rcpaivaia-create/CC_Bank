package br.com.ccbank.servidor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ServidorMainTest {

    @AfterEach
    void limpar() {
        System.clearProperty("ccbank.config");
    }

    @Test
    void falhaNaInicializacaoDevolveCodigoDeErro(@TempDir Path dir) {
        System.setProperty("ccbank.config", dir.resolve("nao-existe.properties").toString());
        assertEquals(1, ServidorMain.executar(new String[] {"--env=v1"}));
    }

    @Test
    void ambienteInvalidoDevolveCodigoDeErro() {
        assertEquals(1, ServidorMain.executar(new String[] {"--env=v9"}));
    }
}
