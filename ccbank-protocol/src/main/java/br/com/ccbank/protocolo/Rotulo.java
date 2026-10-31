package br.com.ccbank.protocolo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Catálogo dos rótulos do protocolo (spec, tabela 4.2). O nome é usado apenas
 * para leitura humana nos logs; no fio trafega somente o código.
 */
public enum Rotulo {

    TRANSACAO("00001", "transacao"),
    OPERADOR("00002", "operador"),
    SENHA_OPERADOR("00003", "senha_operador"),
    COOPERATIVA_OPERADOR("00004", "cooperativa_operador"),
    PAC("00005", "pac"),
    TERMINAL("00006", "terminal"),
    NOME_OPERADOR("00007", "nome_operador"),
    COOPERATIVA_CONTA("00010", "cooperativa_conta"),
    CONTA("00011", "conta"),
    SENHA_CONTA("00012", "senha_conta"),
    COOPERATIVA_DESTINO("00020", "cooperativa_destino"),
    CONTA_DESTINO("00021", "conta_destino"),
    VALOR("00030", "valor"),
    DATA_INICIO("00031", "data_inicio"),
    DATA_FIM("00032", "data_fim"),
    QTD_LANCAMENTOS("00080", "qtd_lancamentos"),
    LANC_DATA_HORA("00081", "lanc_data_hora"),
    LANC_TIPO("00082", "lanc_tipo"),
    LANC_VALOR("00083", "lanc_valor"),
    LANC_SALDO_APOS("00084", "lanc_saldo_apos"),
    LANC_CONTRAPARTE("00085", "lanc_contraparte"),
    CODIGO_RETORNO("00090", "codigo_retorno"),
    MENSAGEM_RETORNO("00091", "mensagem_retorno"),
    SALDO("00092", "saldo"),
    TITULAR("00093", "titular"),
    ID_TRANSACAO("00094", "id_transacao");

    private static final Map<String, Rotulo> POR_CODIGO = new HashMap<>();

    static {
        for (Rotulo r : values()) {
            POR_CODIGO.put(r.codigo, r);
        }
    }

    private final String codigo;
    private final String nome;

    Rotulo(String codigo, String nome) {
        this.codigo = codigo;
        this.nome = nome;
    }

    public String codigo() {
        return codigo;
    }

    public String nome() {
        return nome;
    }

    public static Optional<Rotulo> porCodigo(String codigo) {
        return Optional.ofNullable(POR_CODIGO.get(codigo));
    }
}
