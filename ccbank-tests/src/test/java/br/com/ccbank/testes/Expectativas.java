package br.com.ccbank.testes;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/** Lê o veredito esperado por ataque e lado de {@code expectativas-ataques.properties}. */
final class Expectativas {

    private static final Properties PROPS = carregar();

    private Expectativas() {
    }

    private static Properties carregar() {
        Properties p = new Properties();
        try (InputStream in = Expectativas.class.getResourceAsStream("/expectativas-ataques.properties")) {
            if (in == null) {
                throw new IllegalStateException("expectativas-ataques.properties ausente no classpath");
            }
            p.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return p;
    }

    static Veredito ler(String id, String lado) {
        String chave = id + "." + lado;
        String valor = PROPS.getProperty(chave);
        if (valor == null) {
            throw new IllegalStateException("expectativa ausente para " + chave);
        }
        return Veredito.valueOf(valor.trim());
    }
}
