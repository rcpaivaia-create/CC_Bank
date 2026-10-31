package br.com.ccbank.testes;

import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;

/** Monta requisições como o cliente faria, com o operador rodrigop em 0001/001/001. */
final class Requisicoes {

    static final String OPERADOR = "rodrigop";
    static final String COOPERATIVA = "0001";
    static final String PAC = "001";
    static final String TERMINAL = "001";

    private Requisicoes() {
    }

    static Mensagem login(String login, String senha) {
        return new Mensagem()
                .adicionar(Rotulo.TRANSACAO, "login")
                .adicionar(Rotulo.OPERADOR, login)
                .adicionar(Rotulo.SENHA_OPERADOR, senha);
    }

    /** Transação com o cabeçalho de origem do operador padrão. */
    static Mensagem cabecalho(String transacao) {
        return cabecalho(transacao, OPERADOR, COOPERATIVA, PAC, TERMINAL);
    }

    static Mensagem cabecalho(String transacao, String operador, String coop, String pac, String terminal) {
        return new Mensagem()
                .adicionar(Rotulo.TRANSACAO, transacao)
                .adicionar(Rotulo.OPERADOR, operador)
                .adicionar(Rotulo.COOPERATIVA_OPERADOR, coop)
                .adicionar(Rotulo.PAC, pac)
                .adicionar(Rotulo.TERMINAL, terminal);
    }

    static Mensagem conta(Mensagem m, String coop, String conta, String senha) {
        return m.adicionar(Rotulo.COOPERATIVA_CONTA, coop)
                .adicionar(Rotulo.CONTA, conta)
                .adicionar(Rotulo.SENHA_CONTA, senha);
    }

    static Mensagem saldo(String coop, String conta, String senha) {
        return conta(cabecalho("saldo"), coop, conta, senha);
    }

    static Mensagem saque(String coop, String conta, String senha, String valor) {
        return conta(cabecalho("saque"), coop, conta, senha).adicionar(Rotulo.VALOR, valor);
    }

    static Mensagem deposito(String coop, String conta, String senha, String valor) {
        return conta(cabecalho("deposito"), coop, conta, senha).adicionar(Rotulo.VALOR, valor);
    }

    static Mensagem transferencia(String coop, String conta, String senha,
            String coopDestino, String contaDestino, String valor) {
        return conta(cabecalho("transferencia"), coop, conta, senha)
                .adicionar(Rotulo.COOPERATIVA_DESTINO, coopDestino)
                .adicionar(Rotulo.CONTA_DESTINO, contaDestino)
                .adicionar(Rotulo.VALOR, valor);
    }

    static Mensagem extrato(String coop, String conta, String senha, String inicio, String fim) {
        Mensagem m = conta(cabecalho("extrato"), coop, conta, senha);
        if (inicio != null) {
            m.adicionar(Rotulo.DATA_INICIO, inicio);
        }
        if (fim != null) {
            m.adicionar(Rotulo.DATA_FIM, fim);
        }
        return m;
    }
}
