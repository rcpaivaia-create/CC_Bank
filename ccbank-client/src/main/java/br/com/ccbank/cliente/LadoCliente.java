package br.com.ccbank.cliente;

/** As duas metades do cliente: esquerda fala com o v1, direita com o v2. */
public enum LadoCliente {

    ESQUERDO("ESQ", "ESQUERDO · v1 original", 9001, 8001, "esquerdo"),
    DIREITO("DIR", "DIREITO · v2 evoluções", 9002, 8002, "direito");

    private final String sigla;
    private final String titulo;
    private final int portaServidor;
    private final int portaInterceptador;
    private final String sufixoLog;

    LadoCliente(String sigla, String titulo, int portaServidor, int portaInterceptador, String sufixoLog) {
        this.sigla = sigla;
        this.titulo = titulo;
        this.portaServidor = portaServidor;
        this.portaInterceptador = portaInterceptador;
        this.sufixoLog = sufixoLog;
    }

    /** Identificação nos logs: {@code ESQ} ou {@code DIR}. */
    public String sigla() {
        return sigla;
    }

    public String titulo() {
        return titulo;
    }

    public int portaServidor() {
        return portaServidor;
    }

    public int portaInterceptador() {
        return portaInterceptador;
    }

    /** Sufixo do logger e do arquivo de log: {@code esquerdo} ou {@code direito}. */
    public String sufixoLog() {
        return sufixoLog;
    }

    public int porta(boolean viaInterceptador) {
        return viaInterceptador ? portaInterceptador : portaServidor;
    }

    /** System property {@code ccbank.host}, padrão {@code 127.0.0.1}. */
    public static String host() {
        return System.getProperty("ccbank.host", "127.0.0.1");
    }
}
