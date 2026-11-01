package br.com.ccbank.testes;

import java.util.Collections;

import br.com.ccbank.protocolo.SenhaHash;

/** #9: o MD5 da senha aparece legível no log do interceptador (captura passiva). */
final class CapturaPassiva implements Ataque {

    @Override
    public String id() {
        return "captura-passiva";
    }

    @Override
    public String descricao() {
        return "Senha (MD5) legível no log do interceptador";
    }

    @Override
    public Veredito avaliar(ContextoAtaque contexto) throws Exception {
        String hash = SenhaHash.md5("senha123");
        try (SessaoAtaque s = contexto.abrir(Collections.emptyList())) {
            s.cliente().enviar(Requisicoes.login("rodrigop", "senha123"));
            long fim = System.currentTimeMillis() + 2000;
            while (System.currentTimeMillis() < fim) {
                if (s.logCapturado().stream().anyMatch(linha -> linha.contains(hash))) {
                    return Veredito.VULNERAVEL;
                }
                Thread.sleep(20);
            }
        }
        return Veredito.PROTEGIDO;
    }
}
