package br.com.ccbank.testes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class CatalogoAtaquesTest {

    @Test
    void catalogoTem14AtaquesComIdsUnicos() {
        List<Ataque> todos = CatalogoAtaques.todos();
        assertEquals(14, todos.size());
        assertEquals(14, todos.stream().map(Ataque::id).distinct().count());
    }
}
