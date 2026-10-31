package br.com.ccbank.servidor.config;

/** Os dois lados do laboratório: v1 (original, inseguro de propósito) e v2 (evoluções). */
public enum Ambiente {

    V1(9001, "ccbank_v1", "ccbank_v1_teste", "db/v1"),
    V2(9002, "ccbank_v2", "ccbank_v2_teste", "db/v2");

    private final int porta;
    private final String banco;
    private final String bancoTeste;
    private final String pastaScripts;

    Ambiente(int porta, String banco, String bancoTeste, String pastaScripts) {
        this.porta = porta;
        this.banco = banco;
        this.bancoTeste = bancoTeste;
        this.pastaScripts = pastaScripts;
    }

    public int porta() {
        return porta;
    }

    public String banco() {
        return banco;
    }

    public String bancoTeste() {
        return bancoTeste;
    }

    public String pastaScripts() {
        return pastaScripts;
    }

    /** Identificação do lado nos logs: {@code V1} ou {@code V2}. */
    public String lado() {
        return name();
    }

    public static Ambiente porNome(String nome) {
        for (Ambiente a : values()) {
            if (a.name().equalsIgnoreCase(nome)) {
                return a;
            }
        }
        throw new IllegalArgumentException("ambiente desconhecido: " + nome + " (use v1 ou v2)");
    }
}
