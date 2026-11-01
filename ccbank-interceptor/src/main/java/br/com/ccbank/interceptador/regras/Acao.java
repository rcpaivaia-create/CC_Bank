package br.com.ccbank.interceptador.regras;

import java.util.ArrayList;
import java.util.List;

import br.com.ccbank.protocolo.Campo;

/**
 * Ação de uma regra sobre os campos de uma mensagem.
 *
 * <ul>
 *   <li>{@code SET}: troca o valor de todas as ocorrências do rótulo; rótulo ausente é no-op.</li>
 *   <li>{@code ADD}: acrescenta um campo ao final, sempre.</li>
 *   <li>{@code REMOVE}: remove todas as ocorrências do rótulo.</li>
 * </ul>
 */
public final class Acao {

    public enum Tipo {
        SET, ADD, REMOVE
    }

    private final Tipo tipo;
    private final String rotulo;
    private final String valor;

    public Acao(Tipo tipo, String rotulo, String valor) {
        this.tipo = tipo;
        this.rotulo = rotulo;
        this.valor = valor;
    }

    public Tipo tipo() {
        return tipo;
    }

    public String rotulo() {
        return rotulo;
    }

    public String valor() {
        return valor;
    }

    /** Aplica a ação a {@code campos} (que é mutado) e devolve o que mudou, em ordem. */
    public List<Adulteracao> aplicar(List<Campo> campos, String nomeRegra) {
        List<Adulteracao> adulteracoes = new ArrayList<>();
        switch (tipo) {
            case SET:
                for (int i = 0; i < campos.size(); i++) {
                    Campo c = campos.get(i);
                    if (c.rotulo().equals(rotulo)) {
                        campos.set(i, new Campo(rotulo, valor));
                        adulteracoes.add(new Adulteracao(rotulo, c.valor(), valor, nomeRegra));
                    }
                }
                break;
            case ADD:
                campos.add(new Campo(rotulo, valor));
                adulteracoes.add(new Adulteracao(rotulo, null, valor, nomeRegra));
                break;
            case REMOVE:
                for (int i = campos.size() - 1; i >= 0; i--) {
                    Campo c = campos.get(i);
                    if (c.rotulo().equals(rotulo)) {
                        campos.remove(i);
                        adulteracoes.add(new Adulteracao(rotulo, c.valor(), null, nomeRegra));
                    }
                }
                break;
            default:
                throw new IllegalStateException("tipo não tratado: " + tipo);
        }
        return adulteracoes;
    }
}
