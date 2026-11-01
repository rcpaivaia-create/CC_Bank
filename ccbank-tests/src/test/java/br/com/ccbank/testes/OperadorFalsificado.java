package br.com.ccbank.testes;

/** #5: regra troca o operador da requisição; o jornal grava outro operador. */
final class OperadorFalsificado implements Ataque {

    @Override
    public String id() {
        return "operador-falsificado";
    }

    @Override
    public String descricao() {
        return "Operador da requisição falsificado";
    }

    @Override
    public Veredito avaliar(ContextoAtaque contexto) throws Exception {
        try (SessaoAtaque s = contexto.abrir(CatalogoRegras.regra(id()))) {
            s.cliente().enviar(Requisicoes.login("rodrigop", "senha123"));
            s.cliente().enviar(Requisicoes.saque("0001", "100001", "100.00"));
        }
        int comOutroOperador = BancoTeste.contar(contexto.ds(),
                "select count(*) from transacao where tipo='SAQUE' and operador_login='ana.lima'");
        return comOutroOperador == 1 ? Veredito.VULNERAVEL : Veredito.PROTEGIDO;
    }
}
