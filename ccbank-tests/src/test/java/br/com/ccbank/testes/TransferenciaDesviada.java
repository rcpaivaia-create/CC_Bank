package br.com.ccbank.testes;

import java.math.BigDecimal;

/** #3: regra troca a conta de destino da transferência; outra conta é creditada. */
final class TransferenciaDesviada implements Ataque {

    @Override
    public String id() {
        return "transferencia-desviada";
    }

    @Override
    public String descricao() {
        return "Conta de destino da transferência desviada";
    }

    @Override
    public Veredito avaliar(ContextoAtaque contexto) throws Exception {
        try (SessaoAtaque s = contexto.abrir(CatalogoRegras.regra(id()))) {
            s.cliente().enviar(Requisicoes.login("rodrigop", "senha123"));
            s.cliente().enviar(Requisicoes.transferencia("0001", "100001", "0001", "100002", "100.00"));
        }
        BigDecimal desviada = BancoTeste.saldo(contexto.ds(), "0001", "100010");
        BigDecimal pretendida = BancoTeste.saldo(contexto.ds(), "0001", "100002");
        boolean vulneravel = desviada.compareTo(new BigDecimal("100.00")) == 0
                && pretendida.compareTo(new BigDecimal("500.00")) == 0;
        return vulneravel ? Veredito.VULNERAVEL : Veredito.PROTEGIDO;
    }
}
