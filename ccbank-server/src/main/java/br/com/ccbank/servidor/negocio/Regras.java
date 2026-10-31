package br.com.ccbank.servidor.negocio;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import br.com.ccbank.protocolo.CodigoRetorno;
import br.com.ccbank.protocolo.FormatoValores;
import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;
import br.com.ccbank.servidor.dados.Conta;
import br.com.ccbank.servidor.dados.RegistroJornal;
import br.com.ccbank.servidor.dados.RepositorioContas;

/** Validações comuns às operações, na ordem fixada pelo plano da fase 1b. */
final class Regras {

    /**
     * Cabeçalho de origem e conta exigidos em toda transação que não é login (spec, tabela 4.4).
     * A senha do cliente (00012) só é exigida na {@code validar_senha}.
     */
    static final Rotulo[] ORIGEM = {
        Rotulo.OPERADOR, Rotulo.COOPERATIVA_OPERADOR, Rotulo.PAC, Rotulo.TERMINAL,
        Rotulo.COOPERATIVA_CONTA, Rotulo.CONTA
    };

    private Regras() {
    }

    static void exigir(Mensagem req, Rotulo... rotulos) throws Recusa {
        for (Rotulo r : rotulos) {
            if (!req.primeiro(r).isPresent()) {
                throw new Recusa(CodigoRetorno.MENSAGEM_MALFORMADA);
            }
        }
    }

    static String valor(Mensagem req, Rotulo r) {
        return req.primeiro(r).get();
    }

    /** Valida o 00030 e registra no jornal. */
    static BigDecimal valorOperacao(Mensagem req, RegistroJornal jornal) throws Recusa {
        Optional<BigDecimal> valor = FormatoValores.parseValorOperacao(valor(req, Rotulo.VALOR));
        if (!valor.isPresent()) {
            throw new Recusa(CodigoRetorno.VALOR_INVALIDO);
        }
        jornal.setValor(valor.get());
        return valor.get();
    }

    /**
     * Localiza a conta de origem (00010/00011) e registra no jornal a conta e a marcação Intercredis.
     * Não confere senha: no v1 a operação confia que o cliente chamou {@code validar_senha} antes.
     */
    static Conta localizarConta(Connection c, Mensagem req, RegistroJornal jornal) throws Recusa, SQLException {
        String coop = valor(req, Rotulo.COOPERATIVA_CONTA);
        String numero = valor(req, Rotulo.CONTA);
        jornal.setCooperativaOrigem(coop);
        jornal.setContaOrigem(numero);
        Optional<Conta> conta = RepositorioContas.buscar(c, coop, numero);
        if (!conta.isPresent()) {
            throw new Recusa(CodigoRetorno.CONTA_INEXISTENTE);
        }
        jornal.setContaOrigemId(conta.get().id());
        jornal.setIntercredis(!conta.get().cooperativa().equals(jornal.getCooperativa()));
        return conta.get();
    }

    /** Confere a senha do cliente (00012) com a da conta. */
    static void conferirSenha(Conta conta, Mensagem req) throws Recusa {
        if (!conta.senha().equals(valor(req, Rotulo.SENHA_CONTA))) {
            throw new Recusa(CodigoRetorno.SENHA_CONTA_INVALIDA);
        }
    }
}
