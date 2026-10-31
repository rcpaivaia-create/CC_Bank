package br.com.ccbank.servidor.dados;

/** Operador com o terminal cadastrado (códigos de cooperativa, PAC e terminal). */
public final class Operador {

    private final int id;
    private final String login;
    private final String senha;
    private final String nome;
    private final String cooperativa;
    private final String pac;
    private final String terminal;
    private final boolean ativo;

    public Operador(int id, String login, String senha, String nome,
            String cooperativa, String pac, String terminal, boolean ativo) {
        this.id = id;
        this.login = login;
        this.senha = senha;
        this.nome = nome;
        this.cooperativa = cooperativa;
        this.pac = pac;
        this.terminal = terminal;
        this.ativo = ativo;
    }

    public int id() {
        return id;
    }

    public String login() {
        return login;
    }

    public String senha() {
        return senha;
    }

    public String nome() {
        return nome;
    }

    public String cooperativa() {
        return cooperativa;
    }

    public String pac() {
        return pac;
    }

    public String terminal() {
        return terminal;
    }

    public boolean ativo() {
        return ativo;
    }
}
