package br.com.ccbank.interceptador.mediacao;

import java.util.Collections;

import br.com.ccbank.interceptador.regras.ResultadoAplicacao;
import br.com.ccbank.protocolo.Mensagem;

/**
 * O que o proxy faz com uma mensagem decodificável, decidido por um {@link Mediador}.
 *
 * <ul>
 *   <li>{@code ENCAMINHAR} com {@code reserializar=false}: escreve os bytes originais.</li>
 *   <li>{@code ENCAMINHAR} com {@code reserializar=true}: escreve {@code codificar(resultado.mensagem())}.</li>
 *   <li>{@code DESCARTAR}: não escreve nada.</li>
 * </ul>
 */
public final class DecisaoMediacao {

    public enum Acao {
        ENCAMINHAR, DESCARTAR
    }

    private final Acao acao;
    private final ResultadoAplicacao resultado;
    private final boolean reserializar;

    private DecisaoMediacao(Acao acao, ResultadoAplicacao resultado, boolean reserializar) {
        this.acao = acao;
        this.resultado = resultado;
        this.reserializar = reserializar;
    }

    /** Encaminha a mensagem como veio (bytes originais). */
    public static DecisaoMediacao encaminharOriginal(Mensagem mensagem) {
        return new DecisaoMediacao(Acao.ENCAMINHAR,
                new ResultadoAplicacao(mensagem, Collections.emptyList()), false);
    }

    /** Encaminha a mensagem modificada (o proxy reserializa a mensagem do resultado). */
    public static DecisaoMediacao encaminharModificada(ResultadoAplicacao resultado) {
        return new DecisaoMediacao(Acao.ENCAMINHAR, resultado, true);
    }

    /** Descarta a mensagem: nada é escrito à outra ponta. */
    public static DecisaoMediacao descartar() {
        return new DecisaoMediacao(Acao.DESCARTAR, null, false);
    }

    public Acao acao() {
        return acao;
    }

    public ResultadoAplicacao resultado() {
        return resultado;
    }

    public boolean reserializar() {
        return reserializar;
    }
}
