package br.com.ccbank.testes;

import java.util.Optional;

import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;

/** #7: a regra troca o saldo na resposta; o cliente vê um valor diferente do real. */
final class SaldoExibidoFalso implements Ataque {

    @Override
    public String id() {
        return "saldo-exibido-falso";
    }

    @Override
    public String descricao() {
        return "Saldo exibido ao cliente adulterado na resposta";
    }

    @Override
    public Veredito avaliar(ContextoAtaque contexto) throws Exception {
        Optional<String> saldoExibido;
        try (SessaoAtaque s = contexto.abrir(CatalogoRegras.regra(id()))) {
            s.cliente().enviar(Requisicoes.login("rodrigop", "senha123"));
            Mensagem resp = s.cliente().enviar(Requisicoes.saldo("0001", "100001"));
            saldoExibido = resp.primeiro(Rotulo.SALDO);
        }
        return saldoExibido.equals(Optional.of("9999.99")) ? Veredito.VULNERAVEL : Veredito.PROTEGIDO;
    }
}
