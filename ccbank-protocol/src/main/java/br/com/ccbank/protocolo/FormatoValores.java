package br.com.ccbank.protocolo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Optional;
import java.util.regex.Pattern;

/** Representação textual de valores monetários e datas no protocolo (spec 4.1). */
public final class FormatoValores {

    private static final Pattern VALOR = Pattern.compile("\\d{1,13}(\\.\\d{1,2})?");
    private static final DateTimeFormatter DATA =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");

    private FormatoValores() {
    }

    /** Valor de operação: positivo, ponto decimal, até duas casas e 13 dígitos inteiros. */
    public static Optional<BigDecimal> parseValorOperacao(String texto) {
        if (texto == null || !VALOR.matcher(texto).matches()) {
            return Optional.empty();
        }
        BigDecimal valor = new BigDecimal(texto).setScale(2, RoundingMode.UNNECESSARY);
        return valor.signum() > 0 ? Optional.of(valor) : Optional.empty();
    }

    public static String formatarValor(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    public static Optional<LocalDate> parseData(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(texto, DATA));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    public static String formatarData(LocalDate data) {
        return DATA.format(data);
    }

    public static String formatarDataHora(LocalDateTime dataHora) {
        return DATA_HORA.format(dataHora);
    }
}
