package br.com.ccbank.interceptador.regras;

import java.util.Objects;

/**
 * Registro de uma alteração feita por uma regra num campo, para o log do interceptador.
 *
 * <p>{@code antes == null} indica campo adicionado; {@code depois == null} indica campo removido.
 */
public final class Adulteracao {

    private final String rotulo;
    private final String antes;
    private final String depois;
    private final String regra;

    public Adulteracao(String rotulo, String antes, String depois, String regra) {
        this.rotulo = rotulo;
        this.antes = antes;
        this.depois = depois;
        this.regra = regra;
    }

    public String rotulo() {
        return rotulo;
    }

    public String antes() {
        return antes;
    }

    public String depois() {
        return depois;
    }

    public String regra() {
        return regra;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Adulteracao)) {
            return false;
        }
        Adulteracao outra = (Adulteracao) o;
        return Objects.equals(rotulo, outra.rotulo) && Objects.equals(antes, outra.antes)
                && Objects.equals(depois, outra.depois) && Objects.equals(regra, outra.regra);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rotulo, antes, depois, regra);
    }

    @Override
    public String toString() {
        return "Adulteracao{" + rotulo + ": " + antes + " -> " + depois + " [" + regra + "]}";
    }
}
