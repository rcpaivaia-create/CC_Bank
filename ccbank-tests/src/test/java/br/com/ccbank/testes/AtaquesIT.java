package br.com.ccbank.testes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import br.com.ccbank.servidor.config.Ambiente;

class AtaquesIT {

    @AfterAll
    static void gerarMatriz() throws Exception {
        String html = GeradorMatrizAtaques.gerar(CatalogoAtaques.todos(), ResultadosAtaques.resultados(),
                LocalDateTime.now());
        GeradorMatrizAtaques.escrever(html);
    }

    static Stream<Arguments> casos() {
        List<Arguments> casos = new ArrayList<>();
        for (Ataque ataque : CatalogoAtaques.todos()) {
            for (Ambiente amb : Ambiente.values()) {
                casos.add(Arguments.of(ataque, amb));
            }
        }
        return casos.stream();
    }

    @ParameterizedTest(name = "{0} / {1}")
    @MethodSource("casos")
    void ataqueBateComExpectativa(Ataque ataque, Ambiente amb) throws Exception {
        String lado = amb.lado().toLowerCase(Locale.ROOT);
        Veredito esperado = Expectativas.ler(ataque.id(), lado);
        Veredito real = ataque.avaliar(new ContextoAtaque(amb, BancoTeste.recriar(amb)));
        ResultadosAtaques.registrar(ataque.id(), lado, real);
        assertEquals(esperado, real, ataque.id() + "/" + lado);
    }
}
