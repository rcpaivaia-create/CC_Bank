package br.com.ccbank.servidor.relatorio;

import java.time.LocalDate;

/** Período e filtros do relatório. Filtros de texto vazios ou nulos são ignorados. */
public final class FiltroRelatorio {

    private final LocalDate inicio;
    private final LocalDate fim;
    private String cooperativa;
    private String pac;
    private String terminal;
    private String operador;
    private String tipo;
    private boolean somenteIntercredis;
    private boolean somenteDivergencias;
    private boolean incluirRecusadas = true;

    public FiltroRelatorio(LocalDate inicio, LocalDate fim) {
        this.inicio = inicio;
        this.fim = fim;
    }

    public FiltroRelatorio cooperativa(String valor) {
        this.cooperativa = valor;
        return this;
    }

    public FiltroRelatorio pac(String valor) {
        this.pac = valor;
        return this;
    }

    public FiltroRelatorio terminal(String valor) {
        this.terminal = valor;
        return this;
    }

    public FiltroRelatorio operador(String valor) {
        this.operador = valor;
        return this;
    }

    public FiltroRelatorio tipo(String valor) {
        this.tipo = valor;
        return this;
    }

    public FiltroRelatorio somenteIntercredis(boolean valor) {
        this.somenteIntercredis = valor;
        return this;
    }

    public FiltroRelatorio somenteDivergencias(boolean valor) {
        this.somenteDivergencias = valor;
        return this;
    }

    public FiltroRelatorio incluirRecusadas(boolean valor) {
        this.incluirRecusadas = valor;
        return this;
    }

    public LocalDate getInicio() {
        return inicio;
    }

    public LocalDate getFim() {
        return fim;
    }

    public String getCooperativa() {
        return cooperativa;
    }

    public String getPac() {
        return pac;
    }

    public String getTerminal() {
        return terminal;
    }

    public String getOperador() {
        return operador;
    }

    public String getTipo() {
        return tipo;
    }

    public boolean isSomenteIntercredis() {
        return somenteIntercredis;
    }

    public boolean isSomenteDivergencias() {
        return somenteDivergencias;
    }

    public boolean isIncluirRecusadas() {
        return incluirRecusadas;
    }
}
