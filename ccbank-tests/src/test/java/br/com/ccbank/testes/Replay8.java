package br.com.ccbank.testes;

import java.math.BigDecimal;
import java.util.Collections;

import br.com.ccbank.interceptador.proxy.Replay;
import br.com.ccbank.protocolo.CodecMensagem;

/** #8: um saque capturado é reenviado N vezes direto ao servidor e debitado N vezes. */
final class Replay8 implements Ataque {

    @Override
    public String id() {
        return "replay";
    }

    @Override
    public String descricao() {
        return "Requisição de saque reenviada N vezes (replay)";
    }

    @Override
    public Veredito avaliar(ContextoAtaque contexto) throws Exception {
        byte[] saque = CodecMensagem.codificar(Requisicoes.saque("0001", "100001", "100.00"));
        try (SessaoAtaque s = contexto.abrir(Collections.emptyList())) {
            s.cliente().enviar(Requisicoes.login("rodrigop", "senha123"));
            s.cliente().enviar(Requisicoes.saque("0001", "100001", "100.00"));
            Replay.reenviar(saque, "127.0.0.1", s.portaServidor(), 3);
        }
        BigDecimal saldo = BancoTeste.saldo(contexto.ds(), "0001", "100001");
        return saldo.compareTo(new BigDecimal("600.00")) == 0 ? Veredito.VULNERAVEL : Veredito.PROTEGIDO;
    }
}
