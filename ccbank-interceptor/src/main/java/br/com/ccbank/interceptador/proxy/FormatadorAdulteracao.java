package br.com.ccbank.interceptador.proxy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import br.com.ccbank.interceptador.regras.Adulteracao;
import br.com.ccbank.protocolo.Campo;
import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;

/**
 * Formata uma mensagem para o log do interceptador, marcando os campos adulterados (seção 6
 * da spec-mãe). Mesmo layout de {@code FormatadorLog.formatarCampos}, com a marca anexada.
 */
public final class FormatadorAdulteracao {

    private static final int LARGURA_NOME = 21;
    private static final String RECUO = "    ";

    private FormatadorAdulteracao() {
    }

    public static String formatar(Mensagem m, List<Adulteracao> adulteracoes) {
        Map<String, Adulteracao> alterados = new LinkedHashMap<>();
        List<Adulteracao> removidos = new ArrayList<>();
        for (Adulteracao a : adulteracoes) {
            if (a.depois() == null) {
                removidos.add(a);
            } else {
                alterados.put(a.rotulo(), a);
            }
        }

        StringBuilder sb = new StringBuilder();
        for (Campo c : m.campos()) {
            linha(sb);
            sb.append(RECUO).append(c.rotulo()).append(' ').append(colunaNome(c.rotulo())).append(' ');
            Adulteracao a = alterados.get(c.rotulo());
            if (a == null) {
                sb.append(c.valor());
            } else if (a.antes() == null) {
                sb.append(c.valor()).append("   [ADICIONADO: regra \"").append(a.regra()).append("\"]");
            } else {
                sb.append(a.antes()).append("  →  ").append(a.depois())
                        .append("   [ADULTERADO: regra \"").append(a.regra()).append("\"]");
            }
        }
        for (Adulteracao a : removidos) {
            linha(sb);
            sb.append(RECUO).append(a.rotulo()).append(' ').append(colunaNome(a.rotulo()))
                    .append(" (removido)   [REMOVIDO: regra \"").append(a.regra()).append("\"]");
        }
        return sb.toString();
    }

    private static void linha(StringBuilder sb) {
        if (sb.length() > 0) {
            sb.append('\n');
        }
    }

    private static String colunaNome(String rotulo) {
        StringBuilder nome = new StringBuilder();
        Optional<Rotulo> r = Rotulo.porCodigo(rotulo);
        nome.append(r.isPresent() ? r.get().nome() : "desconhecido").append(' ');
        while (nome.length() < LARGURA_NOME) {
            nome.append('.');
        }
        return nome.toString();
    }
}
