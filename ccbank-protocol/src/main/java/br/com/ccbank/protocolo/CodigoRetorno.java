package br.com.ccbank.protocolo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Códigos de retorno do rótulo 00090 (spec, tabela 4.3). */
public enum CodigoRetorno {

    SUCESSO("000", "sucesso"),
    OPERADOR_INVALIDO("001", "operador ou senha inválidos"),
    SENHA_CONTA_INVALIDA("002", "senha da conta inválida"),
    SALDO_INSUFICIENTE("003", "saldo insuficiente"),
    CONTA_INEXISTENTE("004", "conta inexistente"),
    VALOR_INVALIDO("005", "valor inválido"),
    MENSAGEM_MALFORMADA("006", "mensagem malformada ou campo obrigatório ausente"),
    CONTA_DESTINO_IGUAL("007", "conta destino igual à conta de origem"),
    ERRO_INTERNO("099", "erro interno");

    private static final Map<String, CodigoRetorno> POR_CODIGO = new HashMap<>();

    static {
        for (CodigoRetorno c : values()) {
            POR_CODIGO.put(c.codigo, c);
        }
    }

    private final String codigo;
    private final String descricao;

    CodigoRetorno(String codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public String codigo() {
        return codigo;
    }

    public String descricao() {
        return descricao;
    }

    public static Optional<CodigoRetorno> porCodigo(String codigo) {
        return Optional.ofNullable(POR_CODIGO.get(codigo));
    }
}
