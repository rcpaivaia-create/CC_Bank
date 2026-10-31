package br.com.ccbank.testes;

import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;
import br.com.ccbank.protocolo.SenhaHash;

/**
 * Monta requisições como o cliente faria, com o operador rodrigop em 0001/001/001.
 * A senha do cliente só vai na validação; as operações não a levam. As senhas trafegam
 * em MD5, como o cliente envia; os métodos "Bruta" enviam o valor sem calcular o hash.
 */
final class Requisicoes {

    static final String OPERADOR = "rodrigop";
    static final String COOPERATIVA = "0001";
    static final String PAC = "001";
    static final String TERMINAL = "001";

    private Requisicoes() {
    }

    static Mensagem login(String login, String senha) {
        return loginComSenhaBruta(login, SenhaHash.md5(senha));
    }

    static Mensagem loginComSenhaBruta(String login, String valor) {
        return new Mensagem()
                .adicionar(Rotulo.TRANSACAO, "login")
                .adicionar(Rotulo.OPERADOR, login)
                .adicionar(Rotulo.SENHA_OPERADOR, valor);
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

    static Mensagem conta(Mensagem m, String coop, String conta) {
        return m.adicionar(Rotulo.COOPERATIVA_CONTA, coop)
                .adicionar(Rotulo.CONTA, conta);
    }

    static Mensagem validarSenha(String coop, String conta, String senha) {
        return validarSenhaBruta(coop, conta, SenhaHash.md5(senha));
    }

    static Mensagem validarSenhaBruta(String coop, String conta, String valor) {
        return conta(cabecalho("validar_senha"), coop, conta).adicionar(Rotulo.SENHA_CONTA, valor);
    }

    static Mensagem saldo(String coop, String conta) {
        return conta(cabecalho("saldo"), coop, conta);
    }

    static Mensagem saque(String coop, String conta, String valor) {
        return conta(cabecalho("saque"), coop, conta).adicionar(Rotulo.VALOR, valor);
    }

    static Mensagem deposito(String coop, String conta, String valor) {
        return conta(cabecalho("deposito"), coop, conta).adicionar(Rotulo.VALOR, valor);
    }

    static Mensagem transferencia(String coop, String conta, String coopDestino, String contaDestino, String valor) {
        return conta(cabecalho("transferencia"), coop, conta)
                .adicionar(Rotulo.COOPERATIVA_DESTINO, coopDestino)
                .adicionar(Rotulo.CONTA_DESTINO, contaDestino)
                .adicionar(Rotulo.VALOR, valor);
    }

    static Mensagem extrato(String coop, String conta, String inicio, String fim) {
        Mensagem m = conta(cabecalho("extrato"), coop, conta);
        if (inicio != null) {
            m.adicionar(Rotulo.DATA_INICIO, inicio);
        }
        if (fim != null) {
            m.adicionar(Rotulo.DATA_FIM, fim);
        }
        return m;
    }
}
