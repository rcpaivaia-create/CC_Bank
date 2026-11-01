package br.com.ccbank.interceptador.config;

import java.util.Locale;

/**
 * Lado do interceptador: cada lado escuta uma porta e repassa para um servidor (spec, seção 3.3).
 * Esquerdo escuta 8001 e vai para 9001; direito escuta 8002 e vai para 9002.
 */
public enum LadoInterceptador {

    ESQUERDO(8001, 9001, "esquerdo", "ESQ"),
    DIREITO(8002, 9002, "direito", "DIR");

    private static final String HOST_DESTINO = "127.0.0.1";

    private final int portaEscuta;
    private final int portaDestino;
    private final String sufixoLog;
    private final String sigla;

    LadoInterceptador(int portaEscuta, int portaDestino, String sufixoLog, String sigla) {
        this.portaEscuta = portaEscuta;
        this.portaDestino = portaDestino;
        this.sufixoLog = sufixoLog;
        this.sigla = sigla;
    }

    public int portaEscuta() {
        return portaEscuta;
    }

    public String hostDestino() {
        return HOST_DESTINO;
    }

    public int portaDestino() {
        return portaDestino;
    }

    public String sufixoLog() {
        return sufixoLog;
    }

    public String sigla() {
        return sigla;
    }

    /** @throws IllegalArgumentException se {@code nome} não for um lado conhecido */
    public static LadoInterceptador porNome(String nome) {
        return valueOf(nome.trim().toUpperCase(Locale.ROOT));
    }
}
