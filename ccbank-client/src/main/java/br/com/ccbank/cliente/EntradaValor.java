package br.com.ccbank.cliente;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converte o que o operador digita para o formato do protocolo. É só conveniência da
 * tela: entrada que não se encaixa segue como digitada e o servidor decide.
 */
public final class EntradaValor {

    private static final Pattern MILHAR = Pattern.compile("\\d{1,3}(\\.\\d{3})+");
    private static final Pattern DATA_BR = Pattern.compile("(\\d{2})/(\\d{2})/(\\d{4})");

    private EntradaValor() {
    }

    /** {@code 1.500,00} → {@code 1500.00}; {@code 1.500} → {@code 1500}; demais textos só aparados. */
    public static String normalizar(String digitado) {
        String t = digitado == null ? "" : digitado.trim();
        if (t.indexOf(',') >= 0) {
            return t.replace(".", "").replace(',', '.');
        }
        if (MILHAR.matcher(t).matches()) {
            return t.replace(".", "");
        }
        return t;
    }

    /** {@code DD/MM/AAAA} → {@code AAAA-MM-DD}; vazia → {@code null}; demais textos só aparados. */
    public static String normalizarData(String digitada) {
        String t = digitada == null ? "" : digitada.trim();
        if (t.isEmpty()) {
            return null;
        }
        Matcher m = DATA_BR.matcher(t);
        return m.matches() ? m.group(3) + "-" + m.group(2) + "-" + m.group(1) : t;
    }
}
