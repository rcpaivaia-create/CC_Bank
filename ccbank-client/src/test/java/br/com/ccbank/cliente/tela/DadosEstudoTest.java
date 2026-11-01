package br.com.ccbank.cliente.tela;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DadosEstudoTest {

    @Test
    void contasVemDoSeed() {
        assertEquals(30, DadosEstudo.CONTAS.size());
        ContaEstudo primeira = DadosEstudo.CONTAS.get(0);
        assertEquals("0001", primeira.cooperativa());
        assertEquals("100001", primeira.numero());
        assertEquals("Maria Souza", primeira.titular());
    }

    @Test
    void operadoresVemDoSeed() {
        assertEquals(12, DadosEstudo.OPERADORES.size());
        OperadorEstudo primeiro = DadosEstudo.OPERADORES.get(0);
        assertEquals("rodrigop", primeiro.login());
        assertEquals("Rodrigo P.", primeiro.nome());
    }

    @Test
    void senhasDeEstudo() {
        assertEquals("123456", DadosEstudo.SENHA_CONTA);
        assertEquals("senha123", DadosEstudo.SENHA_OPERADOR);
    }
}
