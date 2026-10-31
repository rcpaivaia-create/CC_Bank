package br.com.ccbank.cliente.tela;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import br.com.ccbank.cliente.Resposta;
import br.com.ccbank.cliente.ResultadoValidado;
import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;

class TextoResultadoTest {

    private static Resposta resposta(String codigo, String texto, long ms) {
        Mensagem m = new Mensagem().adicionar(Rotulo.CODIGO_RETORNO, codigo).adicionar(Rotulo.MENSAGEM_RETORNO, texto);
        return new Resposta(new Mensagem().adicionar(Rotulo.TRANSACAO, "x"), m, ms);
    }

    private static final Resposta VALIDADA = resposta("000", "Senha validada", 12);

    @Test
    void temposComAsDuasEtapas() {
        ResultadoValidado r = new ResultadoValidado(VALIDADA, resposta("000", "Saque efetuado", 30));
        assertEquals("validação 12 ms · operação 30 ms", TextoResultado.tempos(r));
    }

    @Test
    void temposSemOperacao() {
        ResultadoValidado r = new ResultadoValidado(resposta("002", "senha da conta inválida", 12), null);
        assertEquals("validação 12 ms", TextoResultado.tempos(r));
    }

    @Test
    void situacaoSucesso() {
        ResultadoValidado r = new ResultadoValidado(VALIDADA, resposta("000", "Saque efetuado", 30));
        assertEquals("✔ Saque efetuado", TextoResultado.situacao(r));
    }

    @Test
    void situacaoFalhaNaValidacao() {
        ResultadoValidado r = new ResultadoValidado(resposta("002", "senha da conta inválida", 12), null);
        assertEquals("✖ senha da conta inválida", TextoResultado.situacao(r));
    }

    @Test
    void situacaoFalhaNaOperacao() {
        ResultadoValidado r = new ResultadoValidado(VALIDADA, resposta("003", "saldo insuficiente", 30));
        assertEquals("✖ saldo insuficiente", TextoResultado.situacao(r));
    }
}
