package br.com.ccbank.cliente.tela;

import br.com.ccbank.cliente.ResultadoValidado;

/** Textos do resultado de uma operação com validação de senha. */
final class TextoResultado {

    private TextoResultado() {
    }

    /** {@code validação 12 ms · operação 30 ms}, ou só a validação quando a operação não foi enviada. */
    static String tempos(ResultadoValidado r) {
        String texto = "validação " + r.validacao().duracaoMs() + " ms";
        return r.operacao() == null ? texto : texto + " · operação " + r.operacao().duracaoMs() + " ms";
    }

    /** {@code ✔ <texto>} ou {@code ✖ <texto>} da etapa que definiu o resultado. */
    static String situacao(ResultadoValidado r) {
        return (r.sucesso() ? "✔ " : "✖ ") + r.resultadoFinal().texto();
    }
}
