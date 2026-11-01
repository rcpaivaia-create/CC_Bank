package br.com.ccbank.cliente.tela;

/** Uma conta do seed, para o seletor de contas (conveniência de estudo). */
final class ContaEstudo {

    private final String cooperativa;
    private final String numero;
    private final String titular;

    ContaEstudo(String cooperativa, String numero, String titular) {
        this.cooperativa = cooperativa;
        this.numero = numero;
        this.titular = titular;
    }

    String cooperativa() {
        return cooperativa;
    }

    String numero() {
        return numero;
    }

    String titular() {
        return titular;
    }
}
