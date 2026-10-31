package br.com.ccbank.protocolo;

import java.util.Optional;

/**
 * Formata mensagens para os arquivos de log, com o nome legível de cada rótulo
 * (spec, seção 6). Não altera o protocolo; é só para leitura humana.
 */
public final class FormatadorLog {

    private static final int LARGURA_NOME = 21;
    private static final String RECUO = "    ";

    private FormatadorLog() {
    }

    public static String formatarCampos(Mensagem m) {
        StringBuilder sb = new StringBuilder();
        for (Campo c : m.campos()) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(RECUO).append(c.rotulo()).append(' ')
                    .append(colunaNome(c.rotulo())).append(' ')
                    .append(c.valor());
            if (Rotulo.CODIGO_RETORNO.codigo().equals(c.rotulo())) {
                Optional<CodigoRetorno> codigo = CodigoRetorno.porCodigo(c.valor());
                if (codigo.isPresent()) {
                    sb.append(" (").append(codigo.get().descricao()).append(')');
                }
            }
        }
        return sb.toString();
    }

    public static String formatarTroca(String lado, Direcao direcao, String endereco, Mensagem m, Long duracaoMs) {
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(lado).append("] ").append(direcao.name()).append(' ')
                .append(direcao.seta()).append(' ').append(endereco)
                .append(" (").append(m.tamanho()).append(" campos)");
        if (duracaoMs != null) {
            sb.append(" em ").append(duracaoMs).append(" ms");
        }
        return sb.append('\n').append(formatarCampos(m)).toString();
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
