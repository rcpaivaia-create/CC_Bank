package br.com.ccbank.testes;

import java.math.BigDecimal;

/** #1: regra adultera o valor do saque; o banco debita o valor errado. */
final class ValorAdulterado implements Ataque {

    @Override
    public String id() {
        return "valor-adulterado";
    }

    @Override
    public String descricao() {
        return "Valor do saque adulterado na requisição";
    }

    @Override
    public Veredito avaliar(ContextoAtaque contexto) throws Exception {
        try (SessaoAtaque s = contexto.abrir(CatalogoRegras.regra(id()))) {
            s.cliente().enviar(Requisicoes.login("rodrigop", "senha123"));
            s.cliente().enviar(Requisicoes.saque("0001", "100001", "500.00"));
        }
        BigDecimal saldo = BancoTeste.saldo(contexto.ds(), "0001", "100001");
        return saldo.compareTo(new BigDecimal("995.00")) == 0 ? Veredito.VULNERAVEL : Veredito.PROTEGIDO;
    }
}
