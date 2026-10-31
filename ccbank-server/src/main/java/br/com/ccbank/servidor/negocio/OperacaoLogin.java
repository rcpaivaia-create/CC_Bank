package br.com.ccbank.servidor.negocio;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import br.com.ccbank.protocolo.CodigoRetorno;
import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;
import br.com.ccbank.servidor.dados.Operador;
import br.com.ccbank.servidor.dados.RegistroJornal;
import br.com.ccbank.servidor.dados.RepositorioOperadores;

/** Valida operador e senha e devolve o terminal cadastrado. Não cria sessão. */
final class OperacaoLogin implements Operacao {

    @Override
    public String mensagemSucesso() {
        return "Login efetuado";
    }

    @Override
    public Mensagem executar(Connection c, Mensagem req, RegistroJornal jornal) throws Recusa, SQLException {
        Regras.exigir(req, Rotulo.OPERADOR, Rotulo.SENHA_OPERADOR);
        Optional<Operador> op = RepositorioOperadores.buscarPorLogin(c, Regras.valor(req, Rotulo.OPERADOR));
        if (!op.isPresent() || !op.get().ativo() || !op.get().senha().equals(Regras.valor(req, Rotulo.SENHA_OPERADOR))) {
            throw new Recusa(CodigoRetorno.OPERADOR_INVALIDO);
        }
        Operador o = op.get();
        jornal.setCooperativa(o.cooperativa());
        jornal.setPac(o.pac());
        jornal.setTerminal(o.terminal());
        return new Mensagem()
                .adicionar(Rotulo.COOPERATIVA_OPERADOR, o.cooperativa())
                .adicionar(Rotulo.PAC, o.pac())
                .adicionar(Rotulo.TERMINAL, o.terminal())
                .adicionar(Rotulo.NOME_OPERADOR, o.nome());
    }
}
