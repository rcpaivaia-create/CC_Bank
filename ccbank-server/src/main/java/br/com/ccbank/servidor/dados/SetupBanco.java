package br.com.ccbank.servidor.dados;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import br.com.ccbank.servidor.config.Ambiente;
import br.com.ccbank.servidor.config.Caminhos;
import br.com.ccbank.servidor.config.ConfiguracaoBanco;

/**
 * Cria os bancos e aplica os scripts SQL numerados de {@code db/v1} e {@code db/v2}.
 *
 * <p>Uso: {@code SetupBanco [--recriar] [--somente-teste]}. Sem {@code --recriar}, só
 * aplica scripts nos bancos criados agora; bancos existentes não são tocados.
 */
public final class SetupBanco {

    private final ConfiguracaoBanco cfg;

    public SetupBanco(ConfiguracaoBanco cfg) {
        this.cfg = cfg;
    }

    /** @return {@code true} se o banco foi criado agora */
    public boolean criarBancoSeNaoExistir(String banco) throws SQLException {
        try (Connection c = conectar("postgres")) {
            try (PreparedStatement ps = c.prepareStatement("select 1 from pg_database where datname = ?")) {
                ps.setString(1, banco);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return false;
                    }
                }
            }
            try (Statement st = c.createStatement()) {
                st.execute("CREATE DATABASE \"" + banco + "\"");
            }
            return true;
        }
    }

    public void recriarSchema(String banco, Path pastaScripts) throws SQLException, IOException {
        List<Path> scripts = listarScripts(pastaScripts);
        try (Connection c = conectar(banco)) {
            try (Statement st = c.createStatement()) {
                st.execute("DROP SCHEMA IF EXISTS public CASCADE");
                st.execute("CREATE SCHEMA public");
            }
            c.setAutoCommit(false);
            for (Path script : scripts) {
                try (Statement st = c.createStatement()) {
                    for (String comando : comandos(script)) {
                        st.execute(comando);
                    }
                    c.commit();
                } catch (SQLException e) {
                    c.rollback();
                    throw new SQLException("falha em " + script.getFileName() + ": " + e.getMessage(), e);
                }
            }
        }
    }

    private Connection conectar(String banco) throws SQLException {
        return DriverManager.getConnection(cfg.urlJdbc(banco), cfg.usuario(), cfg.senha());
    }

    private static List<Path> listarScripts(Path pasta) throws IOException {
        if (!Files.isDirectory(pasta)) {
            throw new IOException("pasta de scripts não encontrada: " + pasta);
        }
        List<Path> scripts = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(pasta, "*.sql")) {
            for (Path p : ds) {
                scripts.add(p);
            }
        }
        Collections.sort(scripts);
        return scripts;
    }

    /** Divide o script em comandos terminados por {@code ;} no fim da linha; ignora linhas de comentário. */
    static List<String> comandos(Path script) throws IOException {
        List<String> comandos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        for (String linha : Files.readAllLines(script, StandardCharsets.UTF_8)) {
            String t = linha.trim();
            if (t.isEmpty() || t.startsWith("--")) {
                continue;
            }
            atual.append(linha).append('\n');
            if (t.endsWith(";")) {
                String comando = atual.toString().trim();
                comandos.add(comando.substring(0, comando.length() - 1));
                atual.setLength(0);
            }
        }
        if (atual.toString().trim().length() > 0) {
            comandos.add(atual.toString().trim());
        }
        return comandos;
    }

    public static void main(String[] args) {
        List<String> opcoes = Arrays.asList(args);
        boolean recriar = opcoes.contains("--recriar");
        boolean somenteTeste = opcoes.contains("--somente-teste");
        try {
            ConfiguracaoBanco cfg = ConfiguracaoBanco.carregar(ConfiguracaoBanco.arquivoPadrao());
            SetupBanco setup = new SetupBanco(cfg);
            for (Ambiente amb : Ambiente.values()) {
                Path scripts = Caminhos.localizar(amb.pastaScripts());
                List<String> bancos = somenteTeste
                        ? Collections.singletonList(amb.bancoTeste())
                        : Arrays.asList(amb.banco(), amb.bancoTeste());
                for (String banco : bancos) {
                    boolean criado = setup.criarBancoSeNaoExistir(banco);
                    if (criado || recriar) {
                        setup.recriarSchema(banco, scripts);
                        System.out.println(banco + ": " + (criado ? "criado" : "recriado") + " com " + scripts);
                    } else {
                        System.out.println(banco + ": já existe, mantido (use --recriar para recriar)");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Falha no setup do banco: " + e.getMessage());
            System.exit(1);
        }
    }
}
