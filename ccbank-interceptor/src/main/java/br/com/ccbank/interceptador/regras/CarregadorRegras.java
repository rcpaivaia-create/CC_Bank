package br.com.ccbank.interceptador.regras;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Lê um arquivo de regras em JSON (lista de regras, seção 7.2 da spec-mãe). Qualquer problema
 * de conteúdo vira {@link RegrasInvalidasException} com mensagem clara.
 */
public final class CarregadorRegras {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CarregadorRegras() {
    }

    public static List<Regra> carregar(Path arquivo) throws IOException {
        try (InputStream in = Files.newInputStream(arquivo)) {
            return carregar(in);
        }
    }

    public static List<Regra> carregar(InputStream in) throws IOException {
        JsonNode raiz;
        try {
            raiz = MAPPER.readTree(in);
        } catch (IOException e) {
            throw new RegrasInvalidasException("JSON inválido: " + e.getMessage());
        }
        if (raiz == null || !raiz.isArray()) {
            throw new RegrasInvalidasException("a raiz do arquivo de regras deve ser uma lista");
        }
        List<Regra> regras = new ArrayList<>();
        for (JsonNode no : raiz) {
            regras.add(lerRegra(no));
        }
        return regras;
    }

    private static Regra lerRegra(JsonNode no) throws RegrasInvalidasException {
        String nome = texto(no, "nome");
        boolean ativa = !no.has("ativa") || no.get("ativa").asBoolean();
        Sentido sentido = lerSentido(texto(no, "direcao"));
        Condicao condicao = lerCondicao(no.get("quando"));
        List<Acao> acoes = lerAcoes(no.get("acoes"));
        return new Regra(nome, ativa, sentido, condicao, acoes);
    }

    private static Sentido lerSentido(String valor) throws RegrasInvalidasException {
        try {
            return Sentido.porNome(valor);
        } catch (IllegalArgumentException e) {
            throw new RegrasInvalidasException("direcao inválida: " + valor);
        }
    }

    private static Condicao lerCondicao(JsonNode quando) throws RegrasInvalidasException {
        if (quando == null || !quando.isObject()) {
            throw new RegrasInvalidasException("'quando' deve ser um objeto rótulo→valor");
        }
        Map<String, String> padroes = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> it = quando.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            padroes.put(e.getKey(), e.getValue().asText());
        }
        try {
            return new Condicao(padroes);
        } catch (IllegalArgumentException e) {
            throw new RegrasInvalidasException(e.getMessage());
        }
    }

    private static List<Acao> lerAcoes(JsonNode acoes) throws RegrasInvalidasException {
        if (acoes == null || !acoes.isArray()) {
            throw new RegrasInvalidasException("'acoes' deve ser uma lista");
        }
        List<Acao> lista = new ArrayList<>();
        for (JsonNode no : acoes) {
            Acao.Tipo tipo = lerTipo(texto(no, "tipo"));
            String rotulo = texto(no, "rotulo");
            String valor = no.has("valor") ? no.get("valor").asText() : null;
            if (valor == null && tipo != Acao.Tipo.REMOVE) {
                throw new RegrasInvalidasException("ação " + tipo + " em " + rotulo + " exige 'valor'");
            }
            lista.add(new Acao(tipo, rotulo, valor));
        }
        return lista;
    }

    private static Acao.Tipo lerTipo(String valor) throws RegrasInvalidasException {
        try {
            return Acao.Tipo.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new RegrasInvalidasException("tipo de ação inválido: " + valor);
        }
    }

    private static String texto(JsonNode no, String campo) throws RegrasInvalidasException {
        JsonNode valor = no.get(campo);
        if (valor == null || valor.isNull() || valor.asText().isEmpty()) {
            throw new RegrasInvalidasException("campo obrigatório ausente: " + campo);
        }
        return valor.asText();
    }
}
