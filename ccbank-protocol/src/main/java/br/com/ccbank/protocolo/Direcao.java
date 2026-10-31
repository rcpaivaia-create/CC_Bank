package br.com.ccbank.protocolo;

/** Sentido de uma mensagem do ponto de vista de quem registra o log. */
public enum Direcao {

    ENVIADA("→"),
    RECEBIDA("←");

    private final String seta;

    Direcao(String seta) {
        this.seta = seta;
    }

    public String seta() {
        return seta;
    }
}
