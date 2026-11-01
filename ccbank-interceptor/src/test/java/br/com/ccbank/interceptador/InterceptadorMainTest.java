package br.com.ccbank.interceptador;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InterceptadorMainTest {

    @Test
    void semHeadlessRetorna1() {
        assertEquals(1, InterceptadorMain.executar(new String[] {"--lado=esquerdo"}));
    }

    @Test
    void semLadoRetorna1() {
        assertEquals(1, InterceptadorMain.executar(new String[] {"--headless"}));
    }

    @Test
    void ladoInvalidoRetorna1() {
        assertEquals(1, InterceptadorMain.executar(new String[] {"--headless", "--lado=meio"}));
    }

    @Test
    void arquivoDeRegrasInexistenteRetorna1() {
        assertEquals(1, InterceptadorMain.executar(new String[] {"--headless", "--lado=esquerdo", "--regras=nao-existe.json"}));
    }
}
