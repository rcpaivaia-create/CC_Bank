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
        java.util.Optional<String> codigo;
        try (SessaoAtaque s = contexto.abrir(CatalogoRegras.regra(id()))) {
            s.cliente().enviar(Requisicoes.login("rodrigop", "senha123"));
            codigo = s.cliente().enviar(Requisicoes.saque("0001", "100001", "100.00"))
                    .primeiro(br.com.ccbank.protocolo.Rotulo.CODIGO_RETORNO);
        }
        int comOutroOperador = BancoTeste.contar(contexto.ds(),
                "select count(*) from transacao where tipo='SAQUE' and operador_login='ana.lima'");
        // Flip-ável: PROTEGIDO se o v2 recusar (código != 000) ou gravar o operador autenticado.
        boolean vulneravel = codigo.equals(java.util.Optional.of("000")) && comOutroOperador == 1;
        return vulneravel ? Veredito.VULNERAVEL : Veredito.PROTEGIDO;
    }
}
