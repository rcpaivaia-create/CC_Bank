package br.com.ccbank.servidor.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Localiza arquivos do projeto a partir do diretório atual, subindo até 3 níveis. */
public final class Caminhos {

    private static final int NIVEIS = 3;

    private Caminhos() {
    }

    /**
     * Procura {@code relativo} no diretório atual e em até 3 diretórios pais. Se não
     * achar, devolve o caminho relativo ao diretório atual (para a mensagem de erro).
     */
    public static Path localizar(String relativo) {
        Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        for (int i = 0; i <= NIVEIS && dir != null; i++) {
            Path candidato = dir.resolve(relativo);
            if (Files.exists(candidato)) {
                return candidato;
            }
            dir = dir.getParent();
        }
        return Paths.get(System.getProperty("user.dir")).resolve(relativo);
    }
}
