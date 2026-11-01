package br.com.ccbank.testes;

import br.com.ccbank.protocolo.SenhaHash;

/**
 * #13: o MD5 sem salt de uma senha de 6 dígitos é quebrado por força bruta.
 *
 * <p>Ataque offline: ignora o lado (usa {@code SenhaHash.md5} direto). Para flipar para
 * PROTEGIDO na fase 3, precisará atacar a credencial realmente produzida pelo lado sob teste
 * (ex.: hash salgado do banco v2), não a função de hash compartilhada.
 */
final class QuebraHash implements Ataque {

    @Override
    public String id() {
        return "quebra-hash";
    }

    @Override
    public String descricao() {
        return "MD5 sem salt de senha de 6 dígitos quebrado por força bruta";
    }

    @Override
    public Veredito avaliar(ContextoAtaque contexto) {
        String alvo = SenhaHash.md5("123456");
        for (int i = 0; i <= 999999; i++) {
            String candidato = String.format("%06d", i);
            if (SenhaHash.md5(candidato).equals(alvo)) {
                return "123456".equals(candidato) ? Veredito.VULNERAVEL : Veredito.PROTEGIDO;
            }
        }
        return Veredito.PROTEGIDO;
    }
}
