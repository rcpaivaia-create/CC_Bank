package br.com.ccbank.protocolo;

import java.util.Objects;
import java.util.regex.Pattern;

/** Um par rótulo/valor da mensagem. Imutável. */
public final class Campo {

    private static final Pattern ROTULO = Pattern.compile("\\d{5}");

    private final String rotulo;
    private final String valor;

    public Campo(String rotulo, String valor) {
        if (rotulo == null || !ROTULO.matcher(rotulo).matches()) {
            throw new IllegalArgumentException("rótulo deve ter 5 dígitos: " + rotulo);
        }
        if (valor == null) {
            throw new IllegalArgumentException("valor nulo no rótulo " + rotulo);
        }
        if (valor.indexOf('\n') >= 0 || valor.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("valor com quebra de linha no rótulo " + rotulo);
        }
        this.rotulo = rotulo;
        this.valor = valor;
    }

    public String rotulo() {
        return rotulo;
    }

    public String valor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Campo)) {
            return false;
        }
        Campo outro = (Campo) o;
        return rotulo.equals(outro.rotulo) && valor.equals(outro.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rotulo, valor);
    }

    @Override
    public String toString() {
        return rotulo + "=" + valor;
    }
}
