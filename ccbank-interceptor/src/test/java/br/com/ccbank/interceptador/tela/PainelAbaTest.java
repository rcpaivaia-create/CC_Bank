package br.com.ccbank.interceptador.tela;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import br.com.ccbank.interceptador.config.LadoInterceptador;
import br.com.ccbank.interceptador.mediacao.ModoInterceptador;

class PainelAbaTest {

    @Test
    void estadoInicialEhPassagemSemAbrirPorta() {
        PainelAba aba = new PainelAba(LadoInterceptador.ESQUERDO);
        assertEquals(ModoInterceptador.PASSAGEM, aba.modoAtual());
        // não chamamos iniciar(), então nada abre a porta 8001
    }
}
