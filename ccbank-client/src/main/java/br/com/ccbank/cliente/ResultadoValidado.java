package br.com.ccbank.cliente;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Resultado das duas etapas: a validação da senha e, se ela passou, a operação. */
public final class ResultadoValidado {

    private final Resposta validacao;
    private final Resposta operacao;

    public ResultadoValidado(Resposta validacao, Resposta operacao) {
        this.validacao = validacao;
        this.operacao = operacao;
    }

    public Resposta validacao() {
        return validacao;
    }

    /** A resposta da operação, ou {@code null} se a validação falhou e a operação não foi enviada. */
    public Resposta operacao() {
        return operacao;
    }

    /** A operação, ou a validação quando ela falhou. */
    public Resposta resultadoFinal() {
        return operacao != null ? operacao : validacao;
    }

    /** As trocas feitas, na ordem: 1 (só a validação) ou 2. */
    public List<Resposta> respostas() {
        return operacao == null ? Collections.singletonList(validacao) : Arrays.asList(validacao, operacao);
    }

    public boolean sucesso() {
        return resultadoFinal().sucesso();
    }
}
