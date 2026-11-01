package br.com.ccbank.testes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.com.ccbank.servidor.config.Ambiente;

class AtaquesReplayCapturaIT {

    private ContextoAtaque v1() throws Exception {
        return new ContextoAtaque(Ambiente.V1, BancoTeste.recriar(Ambiente.V1));
    }

    @Test
    void replayDebitaVariasVezes() throws Exception {
        ContextoAtaque ctx = v1();
        assertEquals(Veredito.VULNERAVEL, new Replay8().avaliar(ctx));
        // 1 saque pelo proxy + 3 reenvios diretos, de 100.00 cada: 1000 - 400 = 600.00
        assertEquals(new BigDecimal("600.00"), BancoTeste.saldo(ctx.ds(), "0001", "100001"));
    }

    @Test
    void capturaPassivaVeOHashNoLog() throws Exception {
        assertEquals(Veredito.VULNERAVEL, new CapturaPassiva().avaliar(v1()));
    }
}
