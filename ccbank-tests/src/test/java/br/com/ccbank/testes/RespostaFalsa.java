package br.com.ccbank.testes;

import java.util.Optional;

import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;

/** #6: saque sem saldo responde 003; a regra troca a resposta para 000 e engana o cliente. */
final class RespostaFalsa implements Ataque {

    @Override
    public String id() {
        return "resposta-falsa";
    }

    @Override
    public String descricao() {
        return "Resposta de recusa (003) trocada por sucesso (000)";
    }

    @Override
    public Veredito avaliar(ContextoAtaque contexto) throws Exception {
        Optional<String> codigo;
        try (SessaoAtaque s = contexto.abrir(CatalogoRegras.regra(id()))) {
            s.cliente().enviar(Requisicoes.login("rodrigop", "senha123"));
            Mensagem resp = s.cliente().enviar(Requisicoes.saque("0001", "100010", "100.00"));
            codigo = resp.primeiro(Rotulo.CODIGO_RETORNO);
        }
        return codigo.equals(Optional.of("000")) ? Veredito.VULNERAVEL : Veredito.PROTEGIDO;
    }
}
