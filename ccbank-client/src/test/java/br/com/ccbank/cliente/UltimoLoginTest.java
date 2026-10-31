package br.com.ccbank.cliente;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UltimoLoginTest {

    @Test
    void ausenteDevolveVazio(@TempDir Path dir) {
        assertEquals(Optional.empty(), new UltimoLogin(dir.resolve("x.dat")).ler());
    }

    @Test
    void salvaELe(@TempDir Path dir) throws Exception {
        Path arquivo = dir.resolve("x.dat");
        new UltimoLogin(arquivo).salvar("ana.lima");
        assertEquals(Optional.of("ana.lima"), new UltimoLogin(arquivo).ler());
    }

    @Test
    void criaPasta(@TempDir Path dir) throws Exception {
        Path arquivo = dir.resolve("dados").resolve("x.dat");
        new UltimoLogin(arquivo).salvar("rodrigop");
        assertEquals(Optional.of("rodrigop"), new UltimoLogin(arquivo).ler());
    }

    @Test
    void novoLoginSubstitui(@TempDir Path dir) throws Exception {
        UltimoLogin u = new UltimoLogin(dir.resolve("x.dat"));
        u.salvar("a");
        u.salvar("b");
        assertEquals(Optional.of("b"), u.ler());
    }

    @Test
    void arquivoCorrompidoDevolveVazio(@TempDir Path dir) throws Exception {
        Path arquivo = dir.resolve("x.dat");
        Files.write(arquivo, "lixo".getBytes(UTF_8));
        assertEquals(Optional.empty(), new UltimoLogin(arquivo).ler());
    }

    @Test
    void arquivoNaoTemOLoginEmClaro(@TempDir Path dir) throws Exception {
        Path arquivo = dir.resolve("x.dat");
        new UltimoLogin(arquivo).salvar("rodrigop");
        assertFalse(new String(Files.readAllBytes(arquivo), UTF_8).contains("rodrigop"));
    }

    @Test
    void ladosIndependentes() {
        assertNotEquals(UltimoLogin.doLado(LadoCliente.ESQUERDO).arquivo(), UltimoLogin.doLado(LadoCliente.DIREITO).arquivo());
        assertEquals("dados/ultimo-login-esquerdo.dat",
                UltimoLogin.doLado(LadoCliente.ESQUERDO).arquivo().toString().replace('\\', '/'));
    }
}
