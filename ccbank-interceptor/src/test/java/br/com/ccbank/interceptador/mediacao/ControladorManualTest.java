package br.com.ccbank.interceptador.mediacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;

import br.com.ccbank.interceptador.regras.Sentido;
import br.com.ccbank.protocolo.Mensagem;

class ControladorManualTest {

    private static void aguardarAte(BooleanSupplier cond, long ms) throws InterruptedException {
        long fim = System.currentTimeMillis() + ms;
        while (!cond.getAsBoolean() && System.currentTimeMillis() < fim) {
            Thread.sleep(10);
        }
    }

    @Test
    void sentidoForaDoFiltroNaoBloqueia() {
        ControladorManual c = new ControladorManual(ControladorManual.Filtro.RESPOSTAS);
        DecisaoMediacao d = c.intervir(new Mensagem().adicionar("00001", "saque"), Sentido.REQUISICAO);
        assertEquals(DecisaoMediacao.Acao.ENCAMINHAR, d.acao());
        assertFalse(d.reserializar());
    }

    @Test
    void retemEEncaminhaEditada() throws Exception {
        ControladorManual c = new ControladorManual(ControladorManual.Filtro.AMBAS);
        CompletableFuture<DecisaoMediacao> f = CompletableFuture.supplyAsync(
                () -> c.intervir(new Mensagem().adicionar("00030", "500.00"), Sentido.REQUISICAO));
        aguardarAte(() -> c.retida().isPresent(), 2000);
        assertTrue(c.retida().isPresent());
        c.encaminhar(new Mensagem().adicionar("00030", "0.01"));
        DecisaoMediacao d = f.get(2, TimeUnit.SECONDS);
        assertTrue(d.reserializar());
        assertEquals(Optional.of("0.01"), d.resultado().mensagem().primeiro("00030"));
        assertFalse(c.retida().isPresent());
    }

    @Test
    void encaminharOriginalIgnoraEdicao() throws Exception {
        ControladorManual c = new ControladorManual(ControladorManual.Filtro.AMBAS);
        CompletableFuture<DecisaoMediacao> f = CompletableFuture.supplyAsync(
                () -> c.intervir(new Mensagem().adicionar("00030", "500.00"), Sentido.REQUISICAO));
        aguardarAte(() -> c.retida().isPresent(), 2000);
        c.encaminharOriginal();
        DecisaoMediacao d = f.get(2, TimeUnit.SECONDS);
        assertEquals(DecisaoMediacao.Acao.ENCAMINHAR, d.acao());
        assertFalse(d.reserializar());
        assertEquals(Optional.of("500.00"), d.resultado().mensagem().primeiro("00030"));
    }

    @Test
    void resolverSemRetencaoNaoBloqueiaOChamador() throws Exception {
        ControladorManual c = new ControladorManual(ControladorManual.Filtro.AMBAS);
        // Nada retido: encaminhar (chamado da EDT na tela) não pode bloquear.
        CompletableFuture<Void> f = CompletableFuture.runAsync(
                () -> c.encaminhar(new Mensagem().adicionar("00001", "x")));
        f.get(1, TimeUnit.SECONDS);
    }

    @Test
    void descartarDevolveDescarte() throws Exception {
        ControladorManual c = new ControladorManual(ControladorManual.Filtro.AMBAS);
        CompletableFuture<DecisaoMediacao> f = CompletableFuture.supplyAsync(
                () -> c.intervir(new Mensagem().adicionar("00001", "saque"), Sentido.REQUISICAO));
        aguardarAte(() -> c.retida().isPresent(), 2000);
        c.descartar();
        assertEquals(DecisaoMediacao.Acao.DESCARTAR, f.get(2, TimeUnit.SECONDS).acao());
    }
}
