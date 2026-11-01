package br.com.ccbank.testes;

import java.math.BigDecimal;

/** #2: regra infla o valor do depósito; o banco credita a mais. */
final class DepositoInflado implements Ataque {

    @Override
    public String id() {
        return "deposito-inflado";
    }

    @Override
    public String descricao() {
        return "Valor do depósito inflado na requisição";
    }

    @Override
    public Veredito avaliar(ContextoAtaque contexto) throws Exception {
        try (SessaoAtaque s = contexto.abrir(CatalogoRegras.regra(id()))) {
            s.cliente().enviar(Requisicoes.login("rodrigop", "senha123"));
            s.cliente().enviar(Requisicoes.deposito("0001", "100002", "10.00"));
        }
        BigDecimal saldo = BancoTeste.saldo(contexto.ds(), "0001", "100002");
        return saldo.compareTo(new BigDecimal("10500.00")) == 0 ? Veredito.VULNERAVEL : Veredito.PROTEGIDO;
    }
}
