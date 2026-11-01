package br.com.ccbank.testes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Coletor dos vereditos (ataque → lado → veredito) para a matriz gerada ao final. */
final class ResultadosAtaques {

    private static final Map<String, Map<String, Veredito>> RESULTADOS = new ConcurrentHashMap<>();

    private ResultadosAtaques() {
    }

    static void registrar(String id, String lado, Veredito veredito) {
        RESULTADOS.computeIfAbsent(id, k -> new ConcurrentHashMap<>()).put(lado, veredito);
    }

    static Map<String, Map<String, Veredito>> resultados() {
        Map<String, Map<String, Veredito>> copia = new LinkedHashMap<>();
        RESULTADOS.forEach((id, porLado) -> copia.put(id, new LinkedHashMap<>(porLado)));
        return copia;
    }
}
