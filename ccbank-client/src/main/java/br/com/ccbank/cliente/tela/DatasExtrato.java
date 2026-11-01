package br.com.ccbank.cliente.tela;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Intervalo padrão do extrato: dos últimos 30 dias até hoje, no formato DD/MM/AAAA. */
final class DatasExtrato {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private DatasExtrato() {
    }

    static String inicio(LocalDate hoje) {
        return hoje.minusDays(30).format(BR);
    }

    static String fim(LocalDate hoje) {
        return hoje.format(BR);
    }
}
