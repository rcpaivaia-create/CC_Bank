package br.com.ccbank.interceptador.mediacao;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;

import br.com.ccbank.interceptador.regras.Acao;
import br.com.ccbank.interceptador.regras.Condicao;
import br.com.ccbank.interceptador.regras.Regra;
import br.com.ccbank.interceptador.regras.Sentido;
import br.com.ccbank.protocolo.Mensagem;

class MediadorModoTest {

    private static void aguardarAte(BooleanSupplier cond, long ms) throws InterruptedException {
        long fim = System.currentTimeMillis() + ms;
        while (!cond.getAsBoolean() && System.currentTimeMillis() < fim) {
            Thread.sleep(10);
        }
    }

    @Test
    void delegaConformeOModo() {
        MediadorModo mm = new MediadorModo(new ControladorManual(ControladorManual.Filtro.AMBAS));
        assertEquals(ModoInterceptador.PASSAGEM, mm.modo());
        assertFalse(mm.intervir(new Mensagem().adicionar("00001", "saque"), Sentido.REQUISICAO).reserializar());

        mm.carregarRegras(singletonList(new Regra("x", true, Sentido.REQUISICAO,
                new Condicao(singletonMap("00001", "saque")),
                singletonList(new Acao(Acao.Tipo.SET, "00030", "0.01")))));
        mm.modo(ModoInterceptador.REGRAS);
        assertTrue(mm.intervir(new Mensagem().adicionar("00001", "saque").adicionar("00030", "1.00"),
                Sentido.REQUISICAO).reserializar());
    }

    @Test
    void retidaContinuaResolvivelAposTrocarModo() throws Exception {
        ControladorManual manual = new ControladorManual(ControladorManual.Filtro.AMBAS);
        MediadorModo mm = new MediadorModo(manual);
        mm.modo(ModoInterceptador.MANUAL);
        CompletableFuture<DecisaoMediacao> f = CompletableFuture.supplyAsync(
                () -> mm.intervir(new Mensagem().adicionar("00030", "500.00"), Sentido.REQUISICAO));
        aguardarAte(() -> manual.retida().isPresent(), 2000);
        mm.modo(ModoInterceptador.PASSAGEM); // troca de modo com uma retida pendente
        mm.controladorManual().encaminhar(new Mensagem().adicionar("00030", "0.01"));
        DecisaoMediacao d = f.get(2, TimeUnit.SECONDS);
        assertTrue(d.reserializar());
    }
}
