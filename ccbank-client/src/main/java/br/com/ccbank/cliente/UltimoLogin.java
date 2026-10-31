package br.com.ccbank.cliente;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Último login bem-sucedido de uma metade do cliente, guardado num arquivo cifrado ({@link CifraArquivo}). */
public final class UltimoLogin {

    private final Path arquivo;
    private final Logger log;

    public UltimoLogin(Path arquivo) {
        this.arquivo = arquivo;
        this.log = LoggerFactory.getLogger(UltimoLogin.class);
    }

    /** Avisos vão para o log do lado ({@code logs/client-<lado>.log}). */
    public UltimoLogin(Path arquivo, LadoCliente lado) {
        this.arquivo = arquivo;
        this.log = LoggerFactory.getLogger("ccbank.cliente." + lado.sufixoLog());
    }

    /**
     * {@code dados/ultimo-login-esquerdo.dat} ou {@code dados/ultimo-login-direito.dat}, relativo ao
     * diretório atual (a raiz do projeto quando aberto por {@code scripts/start-cliente.ps1}).
     */
    public static UltimoLogin doLado(LadoCliente lado) {
        return new UltimoLogin(Paths.get("dados", "ultimo-login-" + lado.sufixoLog() + ".dat"), lado);
    }

    public Path arquivo() {
        return arquivo;
    }

    /** @return o último login, ou vazio se o arquivo não existir ou não puder ser decifrado */
    public Optional<String> ler() {
        try {
            String conteudo = new String(Files.readAllBytes(arquivo), StandardCharsets.UTF_8);
            return Optional.of(CifraArquivo.decifrar(conteudo));
        } catch (NoSuchFileException e) {
            return Optional.empty();
        } catch (IOException | GeneralSecurityException e) {
            log.warn("Arquivo de último login ignorado ({}): {}", arquivo, e.getMessage());
            return Optional.empty();
        }
    }

    public void salvar(String login) throws IOException {
        Path pasta = arquivo.toAbsolutePath().getParent();
        if (pasta != null) {
            Files.createDirectories(pasta);
        }
        Files.write(arquivo, CifraArquivo.cifrar(login).getBytes(StandardCharsets.UTF_8));
    }
}
