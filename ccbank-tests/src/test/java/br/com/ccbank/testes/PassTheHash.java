package br.com.ccbank.testes;

import java.util.Collections;
import java.util.Optional;

import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;
import br.com.ccbank.protocolo.SenhaHash;

/** #12: o MD5 capturado é reusado como senha num novo login; o servidor o aceita. */
final class PassTheHash implements Ataque {

    @Override
    public String id() {
        return "pass-the-hash";
    }

    @Override
    public String descricao() {
        return "Hash MD5 capturado reusado como senha (pass-the-hash)";
    }

    @Override
    public Veredito avaliar(ContextoAtaque contexto) throws Exception {
        String hashCapturado = SenhaHash.md5("senha123");
        Optional<String> codigo;
        try (SessaoAtaque s = contexto.abrir(Collections.emptyList())) {
            Mensagem resp = s.cliente().enviar(Requisicoes.loginComSenhaBruta("rodrigop", hashCapturado));
            codigo = resp.primeiro(Rotulo.CODIGO_RETORNO);
        }
        return codigo.equals(Optional.of("000")) ? Veredito.VULNERAVEL : Veredito.PROTEGIDO;
    }
}
