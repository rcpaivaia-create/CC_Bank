package br.com.ccbank.testes;

import java.util.Collections;
import java.util.Optional;

import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;

/** #11: o saque é enviado sem validar_senha antes; o v1 o efetiva mesmo assim. */
final class OperacaoSemValidacao implements Ataque {

    @Override
    public String id() {
        return "operacao-sem-validacao";
    }

    @Override
    public String descricao() {
        return "Operação efetivada sem validar_senha prévia";
    }

    @Override
    public Veredito avaliar(ContextoAtaque contexto) throws Exception {
        Optional<String> codigo;
        try (SessaoAtaque s = contexto.abrir(Collections.emptyList())) {
            s.cliente().enviar(Requisicoes.login("rodrigop", "senha123"));
            Mensagem resp = s.cliente().enviar(Requisicoes.saque("0001", "100001", "100.00"));
            codigo = resp.primeiro(Rotulo.CODIGO_RETORNO);
        }
        return codigo.equals(Optional.of("000")) ? Veredito.VULNERAVEL : Veredito.PROTEGIDO;
    }
}
