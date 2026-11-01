package br.com.ccbank.interceptador.regras;

import java.util.ArrayList;
import java.util.List;

import br.com.ccbank.protocolo.Campo;
import br.com.ccbank.protocolo.Mensagem;

/**
 * Aplica, na ordem, as regras ativas de um sentido a uma mensagem. A condição de cada regra
 * é avaliada contra o estado corrente da mensagem (após as regras anteriores). Não muta a
 * mensagem de entrada: devolve uma {@link Mensagem} nova.
 */
public final class MotorRegras {

    private final List<Regra> regras;

    public MotorRegras(List<Regra> regras) {
        this.regras = new ArrayList<>(regras);
    }

    public ResultadoAplicacao aplicar(Mensagem mensagem, Sentido sentido) {
        List<Campo> campos = new ArrayList<>(mensagem.campos());
        List<Adulteracao> adulteracoes = new ArrayList<>();
        for (Regra regra : regras) {
            if (!regra.ativa() || regra.sentido() != sentido) {
                continue;
            }
            if (!regra.condicao().casa(reconstruir(campos))) {
                continue;
            }
            for (Acao acao : regra.acoes()) {
                adulteracoes.addAll(acao.aplicar(campos, regra.nome()));
            }
        }
        return new ResultadoAplicacao(reconstruir(campos), adulteracoes);
    }

    private static Mensagem reconstruir(List<Campo> campos) {
        Mensagem m = new Mensagem();
        for (Campo c : campos) {
            m.adicionar(c.rotulo(), c.valor());
        }
        return m;
    }
}
