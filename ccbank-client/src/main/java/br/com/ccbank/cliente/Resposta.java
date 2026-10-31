package br.com.ccbank.cliente;

import br.com.ccbank.protocolo.CodigoRetorno;
import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;

/** Resposta do servidor junto com a requisição que a gerou e o tempo de ida e volta. */
public final class Resposta {

    private final Mensagem requisicao;
    private final Mensagem mensagem;
    private final long duracaoMs;

    public Resposta(Mensagem requisicao, Mensagem mensagem, long duracaoMs) {
        this.requisicao = requisicao;
        this.mensagem = mensagem;
        this.duracaoMs = duracaoMs;
    }

    public Mensagem requisicao() {
        return requisicao;
    }

    public Mensagem mensagem() {
        return mensagem;
    }

    public long duracaoMs() {
        return duracaoMs;
    }

    /** Código de retorno (00090), ou vazio se ausente. */
    public String codigo() {
        return mensagem.primeiro(Rotulo.CODIGO_RETORNO).orElse("");
    }

    /** Texto de retorno (00091), ou vazio se ausente. */
    public String texto() {
        return mensagem.primeiro(Rotulo.MENSAGEM_RETORNO).orElse("");
    }

    public boolean sucesso() {
        return CodigoRetorno.SUCESSO.codigo().equals(codigo());
    }
}
