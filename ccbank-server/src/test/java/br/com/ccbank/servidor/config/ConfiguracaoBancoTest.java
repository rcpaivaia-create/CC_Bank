package br.com.ccbank.servidor.config;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfiguracaoBancoTest {

    @Test
    void carregaEMontaUrl(@TempDir Path dir) throws IOException {
        Path f = dir.resolve("db.properties");
        Files.write(f, Arrays.asList("db.host=srv", "db.porta=5433", "db.usuario=u", "db.senha=s"), UTF_8);
        ConfiguracaoBanco c = ConfiguracaoBanco.carregar(f);
        assertEquals("jdbc:postgresql://srv:5433/ccbank_v1", c.urlJdbc("ccbank_v1"));
        assertEquals("u", c.usuario());
    }

    @Test
    void usaPadroesDeHostEPorta(@TempDir Path dir) throws IOException {
        Path f = dir.resolve("db.properties");
        Files.write(f, Arrays.asList("db.usuario=u", "db.senha=s"), UTF_8);
        assertEquals("jdbc:postgresql://localhost:5432/x", ConfiguracaoBanco.carregar(f).urlJdbc("x"));
    }

    @Test
    void exigeUsuarioESenha(@TempDir Path dir) throws IOException {
        Path f = dir.resolve("db.properties");
        Files.write(f, Arrays.asList("db.usuario=u", "db.senha="), UTF_8);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> ConfiguracaoBanco.carregar(f));
        assertTrue(e.getMessage().contains("db.senha"));
    }

    @Test
    void toStringNaoExpoeSenha(@TempDir Path dir) throws IOException {
        Path f = dir.resolve("db.properties");
        Files.write(f, Arrays.asList("db.usuario=u", "db.senha=segredo123"), UTF_8);
        assertFalse(ConfiguracaoBanco.carregar(f).toString().contains("segredo123"));
    }

    @Test
    void ambientePorNome() {
        assertEquals(Ambiente.V1, Ambiente.porNome("v1"));
        assertEquals(9002, Ambiente.porNome("V2").porta());
        assertEquals("ccbank_v2_teste", Ambiente.V2.bancoTeste());
        assertThrows(IllegalArgumentException.class, () -> Ambiente.porNome("v3"));
    }
}
