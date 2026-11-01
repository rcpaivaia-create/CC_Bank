package br.com.ccbank.testes;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import br.com.ccbank.interceptador.regras.CarregadorRegras;
import br.com.ccbank.interceptador.regras.Regra;

/** Acesso ao catálogo de regras de ataque em {@code regras/ataques-exemplo.json} (raiz do repo). */
final class CatalogoRegras {

    private static final String RELATIVO = "regras/ataques-exemplo.json";

    private CatalogoRegras() {
    }

    /** Localiza o catálogo subindo a árvore a partir de {@code user.dir} (CWD do failsafe = módulo). */
    static Path arquivo() {
        Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (dir != null) {
            Path candidato = dir.resolve(RELATIVO);
            if (Files.exists(candidato)) {
                return candidato;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("não encontrei " + RELATIVO + " subindo de " + System.getProperty("user.dir"));
    }

    /** @return a regra de nome {@code nome} como lista de um elemento; erro se não existir */
    static List<Regra> regra(String nome) {
        List<Regra> encontradas;
        try {
            encontradas = CarregadorRegras.carregar(arquivo()).stream()
                    .filter(r -> r.nome().equals(nome))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (encontradas.isEmpty()) {
            throw new IllegalArgumentException("regra não encontrada no catálogo: " + nome);
        }
        return encontradas;
    }
}
