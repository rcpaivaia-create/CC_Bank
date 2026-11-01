package br.com.ccbank.interceptador.regras;

import java.util.Locale;

/**
 * Sentido de uma mensagem do ponto de vista de uma regra do interceptador: do cliente para
 * o servidor (requisição) ou do servidor para o cliente (resposta).
 *
 * <p>Distinto de {@link br.com.ccbank.protocolo.Direcao}, que é usado só na formatação de log.
 */
public enum Sentido {

    REQUISICAO,
    RESPOSTA;

    /** @throws IllegalArgumentException se {@code nome} não for um sentido conhecido */
    public static Sentido porNome(String nome) {
        return valueOf(nome.trim().toUpperCase(Locale.ROOT));
    }
}
