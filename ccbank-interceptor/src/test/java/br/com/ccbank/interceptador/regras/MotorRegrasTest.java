package br.com.ccbank.interceptador.regras;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import br.com.ccbank.protocolo.Mensagem;

class MotorRegrasTest {

    private static Regra regra(String nome, boolean ativa, Sentido s, Condicao c, Acao... a) {
        return new Regra(nome, ativa, s, c, asList(a));
    }

    private static Condicao quando(String rotulo, String valor) {
        return new Condicao(singletonMap(rotulo, valor));
    }

    @Test
    void aplicaRegraDoSentidoQueCasa() {
        MotorRegras m = new MotorRegras(singletonList(regra("saque-centavos", true, Sentido.REQUISICAO,
                quando("00001", "saque"), new Acao(Acao.Tipo.SET, "00030", "0.01"))));
        ResultadoAplicacao r = m.aplicar(
                new Mensagem().adicionar("00001", "saque").adicionar("00030", "500.00"), Sentido.REQUISICAO);
        assertEquals(Optional.of("0.01"), r.mensagem().primeiro("00030"));
        assertTrue(r.houveAdulteracao());
    }

    @Test
    void ignoraRegraInativa() {
        MotorRegras m = new MotorRegras(singletonList(regra("x", false, Sentido.REQUISICAO,
                quando("00001", "saque"), new Acao(Acao.Tipo.SET, "00030", "0.01"))));
        ResultadoAplicacao r = m.aplicar(
                new Mensagem().adicionar("00001", "saque").adicionar("00030", "500.00"), Sentido.REQUISICAO);
        assertEquals(Optional.of("500.00"), r.mensagem().primeiro("00030"));
        assertFalse(r.houveAdulteracao());
    }

    @Test
    void ignoraSentidoOposto() {
        MotorRegras m = new MotorRegras(singletonList(regra("x", true, Sentido.RESPOSTA,
                quando("00001", "saque"), new Acao(Acao.Tipo.SET, "00030", "0.01"))));
        ResultadoAplicacao r = m.aplicar(
                new Mensagem().adicionar("00001", "saque").adicionar("00030", "500.00"), Sentido.REQUISICAO);
        assertEquals(Optional.of("500.00"), r.mensagem().primeiro("00030"));
        assertFalse(r.houveAdulteracao());
    }

    @Test
    void ignoraCondicaoQueNaoCasa() {
        MotorRegras m = new MotorRegras(singletonList(regra("x", true, Sentido.REQUISICAO,
                quando("00001", "saque"), new Acao(Acao.Tipo.SET, "00030", "0.01"))));
        ResultadoAplicacao r = m.aplicar(
                new Mensagem().adicionar("00001", "deposito").adicionar("00030", "500.00"), Sentido.REQUISICAO);
        assertEquals(Optional.of("500.00"), r.mensagem().primeiro("00030"));
        assertFalse(r.houveAdulteracao());
    }

    @Test
    void semRegrasAplicaveisDevolveMensagemIntactaEListaVazia() {
        ResultadoAplicacao r = new MotorRegras(emptyList())
                .aplicar(new Mensagem().adicionar("00001", "saque"), Sentido.REQUISICAO);
        assertFalse(r.houveAdulteracao());
        assertEquals(Optional.of("saque"), r.mensagem().primeiro("00001"));
    }

    @Test
    void regrasAplicadasNaOrdemESegundaVeOEstadoDaPrimeira() {
        Regra r1 = regra("set", true, Sentido.REQUISICAO, quando("00001", "saque"),
                new Acao(Acao.Tipo.SET, "00030", "100.00"));
        Regra r2 = regra("marca", true, Sentido.REQUISICAO, quando("00030", "100.00"),
                new Acao(Acao.Tipo.ADD, "00099", "marcado"));
        ResultadoAplicacao r = new MotorRegras(asList(r1, r2)).aplicar(
                new Mensagem().adicionar("00001", "saque").adicionar("00030", "500.00"), Sentido.REQUISICAO);
        assertEquals(Optional.of("marcado"), r.mensagem().primeiro("00099"));
    }

    @Test
    void naoMutaAMensagemDeEntrada() {
        Mensagem entrada = new Mensagem().adicionar("00001", "saque").adicionar("00030", "500.00");
        new MotorRegras(singletonList(regra("x", true, Sentido.REQUISICAO,
                quando("00001", "saque"), new Acao(Acao.Tipo.SET, "00030", "0.01"))))
                .aplicar(entrada, Sentido.REQUISICAO);
        assertEquals(Optional.of("500.00"), entrada.primeiro("00030"));
    }
}
