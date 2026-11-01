package br.com.ccbank.testes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.com.ccbank.servidor.config.Ambiente;

class AtaquesRespostaFluxoIT {

    private ContextoAtaque v1() throws Exception {
        return new ContextoAtaque(Ambiente.V1, BancoTeste.recriar(Ambiente.V1));
    }

    @Test
    void respostaFalsaEnganaOCliente() throws Exception {
        ContextoAtaque ctx = v1();
        assertEquals(Veredito.VULNERAVEL, new RespostaFalsa().avaliar(ctx));
        assertEquals(new BigDecimal("0.00"), BancoTeste.saldo(ctx.ds(), "0001", "100010"));
    }

    @Test
    void saldoExibidoFalso() throws Exception {
        ContextoAtaque ctx = v1();
        assertEquals(Veredito.VULNERAVEL, new SaldoExibidoFalso().avaliar(ctx));
        assertEquals(new BigDecimal("1000.00"), BancoTeste.saldo(ctx.ds(), "0001", "100001"));
    }

    @Test
    void validacaoFalsificada() throws Exception {
        assertEquals(Veredito.VULNERAVEL, new ValidacaoFalsificada().avaliar(v1()));
    }

    @Test
    void operacaoSemValidacao() throws Exception {
        ContextoAtaque ctx = v1();
        assertEquals(Veredito.VULNERAVEL, new OperacaoSemValidacao().avaliar(ctx));
        assertEquals(new BigDecimal("900.00"), BancoTeste.saldo(ctx.ds(), "0001", "100001"));
    }

    @Test
    void passTheHash() throws Exception {
        assertEquals(Veredito.VULNERAVEL, new PassTheHash().avaliar(v1()));
    }
}
