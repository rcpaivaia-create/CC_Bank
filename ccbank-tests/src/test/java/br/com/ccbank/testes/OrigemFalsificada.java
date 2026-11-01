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
        try (SessaoAtaque s = contexto.abrir(CatalogoRegras.regra(id()))) {
            s.cliente().enviar(Requisicoes.login("rodrigop", "senha123"));
            s.cliente().enviar(Requisicoes.saque("0001", "100001", "100.00"));
        }
        int forjadas = BancoTeste.contar(contexto.ds(),
                "select count(*) from transacao where tipo='SAQUE' and cooperativa='9999' and terminal='999'");
        return forjadas == 1 ? Veredito.VULNERAVEL : Veredito.PROTEGIDO;
    }
}
