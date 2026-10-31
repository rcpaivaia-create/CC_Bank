package br.com.ccbank.testes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import br.com.ccbank.servidor.config.Ambiente;
import br.com.ccbank.servidor.config.Caminhos;

class SetupBancoIT {

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void recriaSchemaComSeed(Ambiente amb) throws Exception {
        DataSource ds = BancoTeste.recriar(amb);
        assertEquals(3, BancoTeste.contar(ds, "select count(*) from cooperativa"));
        assertEquals(12, BancoTeste.contar(ds, "select count(*) from terminal"));
        assertEquals(12, BancoTeste.contar(ds, "select count(*) from operador where ativo and senha = 'senha123'"));
        assertEquals(30, BancoTeste.contar(ds, "select count(*) from conta where senha = '123456'"));
        assertEquals(new BigDecimal("1000.00"), BancoTeste.saldo(ds, "0001", "100001"));
        assertEquals(new BigDecimal("0.00"), BancoTeste.saldo(ds, "0001", "100010"));
        assertEquals(0, BancoTeste.contar(ds, "select count(*) from transacao"));
    }

    @Test
    void scriptsV2SaoCopiaDoV1() throws IOException {
        for (String f : new String[] {"001_schema.sql", "002_seed.sql"}) {
            Path v1 = Caminhos.localizar("db/v1/" + f);
            Path v2 = Caminhos.localizar("db/v2/" + f);
            assertArrayEquals(Files.readAllBytes(v1), Files.readAllBytes(v2), f);
        }
    }
}
