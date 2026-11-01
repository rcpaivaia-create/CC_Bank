package br.com.ccbank.testes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import br.com.ccbank.protocolo.Rotulo;
import br.com.ccbank.servidor.config.Ambiente;

class ContextoAtaqueIT {

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void sessaoEntregaLoginPeloProxy(Ambiente amb) throws Exception {
        ContextoAtaque ctx = new ContextoAtaque(amb, BancoTeste.recriar(amb));
        try (SessaoAtaque s = ctx.abrir(Collections.emptyList())) {
            assertEquals(Optional.of("000"),
                    s.cliente().enviar(Requisicoes.login("rodrigop", "senha123")).primeiro(Rotulo.CODIGO_RETORNO));
        }
    }
}
