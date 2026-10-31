package br.com.ccbank.servidor.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/** Credenciais do PostgreSQL lidas de {@code config/db.properties}. */
public final class ConfiguracaoBanco {

    private final String host;
    private final String porta;
    private final String usuario;
    private final String senha;

    private ConfiguracaoBanco(String host, String porta, String usuario, String senha) {
        this.host = host;
        this.porta = porta;
        this.usuario = usuario;
        this.senha = senha;
    }

    public static ConfiguracaoBanco carregar(Path arquivo) throws IOException {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(arquivo);
                Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            p.load(r);
        }
        return new ConfiguracaoBanco(
                valor(p, "db.host", "localhost"),
                valor(p, "db.porta", "5432"),
                obrigatorio(p, "db.usuario", arquivo),
                obrigatorio(p, "db.senha", arquivo));
    }

    /** System property {@code ccbank.config}, ou {@code config/db.properties} localizado a partir do diretório atual. */
    public static Path arquivoPadrao() {
        String definido = System.getProperty("ccbank.config");
        return definido != null ? Paths.get(definido) : Caminhos.localizar("config/db.properties");
    }

    public String urlJdbc(String banco) {
        return "jdbc:postgresql://" + host + ":" + porta + "/" + banco;
    }

    public String usuario() {
        return usuario;
    }

    public String senha() {
        return senha;
    }

    public DataSource criarPool(String banco, int tamanhoMaximo) {
        HikariConfig c = new HikariConfig();
        c.setJdbcUrl(urlJdbc(banco));
        c.setUsername(usuario);
        c.setPassword(senha);
        c.setMaximumPoolSize(tamanhoMaximo);
        c.setAutoCommit(true);
        c.setConnectionInitSql("SET TIME ZONE 'America/Sao_Paulo'");
        c.setPoolName("ccbank-" + banco);
        return new HikariDataSource(c);
    }

    @Override
    public String toString() {
        return "ConfiguracaoBanco[" + usuario + "@" + host + ":" + porta + "]";
    }

    private static String valor(Properties p, String chave, String padrao) {
        String v = p.getProperty(chave);
        return v == null || v.trim().isEmpty() ? padrao : v.trim();
    }

    private static String obrigatorio(Properties p, String chave, Path arquivo) {
        String v = p.getProperty(chave);
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalStateException("preencha " + chave + " em " + arquivo);
        }
        return v.trim();
    }
}
