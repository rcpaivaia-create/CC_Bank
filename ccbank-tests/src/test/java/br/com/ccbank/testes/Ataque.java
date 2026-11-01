package br.com.ccbank.testes;

/** Um ataque do catálogo: roda um cenário contra um lado e devolve o veredito. */
interface Ataque {

    String id();

    String descricao();

    Veredito avaliar(ContextoAtaque contexto) throws Exception;
}
