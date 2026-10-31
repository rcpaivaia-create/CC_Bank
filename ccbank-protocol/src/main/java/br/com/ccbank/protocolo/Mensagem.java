package br.com.ccbank.protocolo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Mensagem do protocolo: lista ordenada de campos. Rótulos podem se repetir
 * (grupos do extrato) e rótulos desconhecidos são preservados.
 */
public final class Mensagem {

    private final List<Campo> campos = new ArrayList<>();

    public Mensagem adicionar(String rotulo, String valor) {
        campos.add(new Campo(rotulo, valor));
        return this;
    }

    public Mensagem adicionar(Rotulo rotulo, String valor) {
        return adicionar(rotulo.codigo(), valor);
    }

    public Optional<String> primeiro(String rotulo) {
        for (Campo c : campos) {
            if (c.rotulo().equals(rotulo)) {
                return Optional.of(c.valor());
            }
        }
        return Optional.empty();
    }

    public Optional<String> primeiro(Rotulo rotulo) {
        return primeiro(rotulo.codigo());
    }

    public List<String> todos(String rotulo) {
        List<String> valores = new ArrayList<>();
        for (Campo c : campos) {
            if (c.rotulo().equals(rotulo)) {
                valores.add(c.valor());
            }
        }
        return valores;
    }

    public List<Campo> campos() {
        return Collections.unmodifiableList(campos);
    }

    public int tamanho() {
        return campos.size();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Mensagem && campos.equals(((Mensagem) o).campos));
    }

    @Override
    public int hashCode() {
        return campos.hashCode();
    }

    @Override
    public String toString() {
        return campos.toString();
    }
}
