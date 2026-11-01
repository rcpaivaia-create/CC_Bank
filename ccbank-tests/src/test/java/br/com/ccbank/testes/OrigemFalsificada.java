package br.com.ccbank.testes;

/** #4: regra forja cooperativa/PAC/terminal; o jornal grava a origem divergente. */
final class OrigemFalsificada implements Ataque {

    @Override
    public String id() {
        return "origem-falsificada";
    }

    @Override
    public String descricao() {
        return "Cooperativa/PAC/terminal de origem falsificados";
    }

    @Override
    public Veredito avaliar(ContextoAtaque contexto) throws Exception {
        java.util.Optional<String> codigo;
        try (SessaoAtaque s = contexto.abrir(CatalogoRegras.regra(id()))) {
            s.cliente().enviar(Requisicoes.login("rodrigop", "senha123"));
            codigo = s.cliente().enviar(Requisicoes.saque("0001", "100001", "100.00"))
                    .primeiro(br.com.ccbank.protocolo.Rotulo.CODIGO_RETORNO);
        }
        int forjadas = BancoTeste.contar(contexto.ds(),
                "select count(*) from transacao where tipo='SAQUE' and cooperativa='9999' and terminal='999'");
        // Flip-ável: PROTEGIDO se o v2 recusar (código != 000) ou gravar a origem autenticada (forjadas == 0).
        boolean vulneravel = codigo.equals(java.util.Optional.of("000")) && forjadas == 1;
        return vulneravel ? Veredito.VULNERAVEL : Veredito.PROTEGIDO;
    }
}
