package br.com.ccbank.interceptador.regras;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import br.com.ccbank.protocolo.Mensagem;

/**
 * Condição de uma regra: para cada rótulo, um padrão que a primeira ocorrência do rótulo
 * deve satisfazer. Padrão literal compara por igualdade; padrão com prefixo {@code ~} é
 * regex avaliada com {@link java.util.regex.Matcher#matches()} (casa o valor inteiro).
 * Todos os rótulos precisam casar.
 */
public final class Condicao {

    private static final String PREFIXO_REGEX = "~";

    private final Map<String, String> padroes;
    private final Map<String, Pattern> compilados;

    /** @throws IllegalArgumentException se algum padrão {@code ~} não for uma regex válida */
    public Condicao(Map<String, String> padroes) {
        this.padroes = new LinkedHashMap<>(padroes);
        this.compilados = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : this.padroes.entrySet()) {
            String padrao = e.getValue();
            if (padrao.startsWith(PREFIXO_REGEX)) {
                try {
                    compilados.put(e.getKey(), Pattern.compile(padrao.substring(PREFIXO_REGEX.length())));
                } catch (PatternSyntaxException ex) {
                    throw new IllegalArgumentException("regex inválida em " + e.getKey() + ": " + ex.getMessage(), ex);
                }
            }
        }
    }

    public boolean casa(Mensagem m) {
        for (Map.Entry<String, String> e : padroes.entrySet()) {
            Optional<String> valor = m.primeiro(e.getKey());
            if (!valor.isPresent()) {
                return false;
            }
            Pattern regex = compilados.get(e.getKey());
            boolean ok = regex != null ? regex.matcher(valor.get()).matches() : valor.get().equals(e.getValue());
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    public Map<String, String> padroes() {
        return new LinkedHashMap<>(padroes);
    }
}
