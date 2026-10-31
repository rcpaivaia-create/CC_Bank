package br.com.ccbank.cliente;

/** Um lançamento do extrato, com os textos exatamente como vieram do servidor. */
public final class LinhaExtrato {

    private final String dataHora;
    private final String tipo;
    private final String valor;
    private final String saldoApos;
    private final String contraparte;

    public LinhaExtrato(String dataHora, String tipo, String valor, String saldoApos, String contraparte) {
        this.dataHora = dataHora;
        this.tipo = tipo;
        this.valor = valor;
        this.saldoApos = saldoApos;
        this.contraparte = contraparte;
    }

    public String dataHora() {
        return dataHora;
    }

    public String tipo() {
        return tipo;
    }

    public String valor() {
        return valor;
    }

    public String saldoApos() {
        return saldoApos;
    }

    public String contraparte() {
        return contraparte;
    }
}
