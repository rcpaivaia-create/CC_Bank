package br.com.ccbank.testes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.com.ccbank.servidor.config.Ambiente;

class AtaquesRequisicaoIT {

    private ContextoAtaque v1() throws Exception {
        return new ContextoAtaque(Ambiente.V1, BancoTeste.recriar(Ambiente.V1));
    }

    @Test
    void valorAdulteradoDebitaValorErrado() throws Exception {
        ContextoAtaque ctx = v1();
        assertEquals(Veredito.VULNERAVEL, new ValorAdulterado().avaliar(ctx));
        assertEquals(new BigDecimal("995.00"), BancoTeste.saldo(ctx.ds(), "0001", "100001"));
    }

    @Test
    void depositoInfladoCreditaAMais() throws Exception {
        ContextoAtaque ctx = v1();
        assertEquals(Veredito.VULNERAVEL, new DepositoInflado().avaliar(ctx));
        assertEquals(new BigDecimal("10500.00"), BancoTeste.saldo(ctx.ds(), "0001", "100002"));
    }

    @Test
    void transferenciaDesviadaCreditaOutraConta() throws Exception {
        ContextoAtaque ctx = v1();
        assertEquals(Veredito.VULNERAVEL, new TransferenciaDesviada().avaliar(ctx));
        assertEquals(new BigDecimal("100.00"), BancoTeste.saldo(ctx.ds(), "0001", "100010"));
        assertEquals(new BigDecimal("500.00"), BancoTeste.saldo(ctx.ds(), "0001", "100002"));
    }

    @Test
    void origemFalsificadaGravaOrigemForjada() throws Exception {
        ContextoAtaque ctx = v1();
        assertEquals(Veredito.VULNERAVEL, new OrigemFalsificada().avaliar(ctx));
        assertEquals(1, BancoTeste.contar(ctx.ds(),
                "select count(*) from transacao where tipo='SAQUE' and cooperativa='9999' and terminal='999'"));
    }

    @Test
    void operadorFalsificadoGravaOutroOperador() throws Exception {
        ContextoAtaque ctx = v1();
        assertEquals(Veredito.VULNERAVEL, new OperadorFalsificado().avaliar(ctx));
        assertEquals(1, BancoTeste.contar(ctx.ds(),
                "select count(*) from transacao where tipo='SAQUE' and operador_login='ana.lima'"));
    }
}
