package br.com.ccbank.testes;

import java.util.Optional;

import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;

/** #10: validar_senha com senha errada responde 002; a regra troca por 000 e o cliente segue. */
final class ValidacaoFalsificada implements Ataque {

    @Override
    public String id() {
        return "validacao-falsificada";
    }

    @Override
    public String descricao() {
        return "Validação de senha (002) trocada por sucesso (000)";
    }

    @Override
    public Veredito avaliar(ContextoAtaque contexto) throws Exception {
        Optional<String> codigo;
        try (SessaoAtaque s = contexto.abrir(CatalogoRegras.regra(id()))) {
            s.cliente().enviar(Requisicoes.login("rodrigop", "senha123"));
            Mensagem resp = s.cliente().enviar(Requisicoes.validarSenha("0001", "100001", "senha-errada"));
            codigo = resp.primeiro(Rotulo.CODIGO_RETORNO);
        }
        return codigo.equals(Optional.of("000")) ? Veredito.VULNERAVEL : Veredito.PROTEGIDO;
    }
}
