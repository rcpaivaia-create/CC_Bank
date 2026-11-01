package br.com.ccbank.cliente.tela;

/** Um operador do seed, para o seletor de operadores (conveniência de estudo). */
final class OperadorEstudo {

    private final String login;
    private final String nome;

    OperadorEstudo(String login, String nome) {
        this.login = login;
        this.nome = nome;
    }

    String login() {
        return login;
    }

    String nome() {
        return nome;
    }
}
