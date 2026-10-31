package br.com.ccbank.cliente;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;

/**
 * Operações do cliente sobre o protocolo. Guarda o operador logado e o terminal devolvido
 * no login, e os repete em toda mensagem (v1: não há sessão no servidor).
 */
public final class ClienteBanco {

    private final LadoCliente lado;
    private final ConexaoServidor conexao;
    private String operador;
    private String nomeOperador;
    private String cooperativa;
    private String pac;
    private String terminal;

    public ClienteBanco(LadoCliente lado, boolean viaInterceptador) {
        this(lado, new ConexaoServidor(lado, LadoCliente.host(), lado.porta(viaInterceptador)));
    }

    public ClienteBanco(LadoCliente lado, ConexaoServidor conexao) {
        this.lado = lado;
        this.conexao = conexao;
    }

    public LadoCliente lado() {
        return lado;
    }

    public String endereco() {
        return conexao.endereco();
    }

    public Resposta login(String login, String senha) throws IOException {
        Mensagem req = new Mensagem()
                .adicionar(Rotulo.TRANSACAO, "login")
                .adicionar(Rotulo.OPERADOR, login)
                .adicionar(Rotulo.SENHA_OPERADOR, senha);
        Resposta r = conexao.enviar(req);
        if (r.sucesso()) {
            Mensagem m = r.mensagem();
            operador = login;
            nomeOperador = m.primeiro(Rotulo.NOME_OPERADOR).orElse(login);
            cooperativa = m.primeiro(Rotulo.COOPERATIVA_OPERADOR).orElse("");
            pac = m.primeiro(Rotulo.PAC).orElse("");
            terminal = m.primeiro(Rotulo.TERMINAL).orElse("");
        }
        return r;
    }

    public boolean logado() {
        return operador != null;
    }

    public String operador() {
        return operador;
    }

    public String nomeOperador() {
        return nomeOperador;
    }

    public String cooperativa() {
        return cooperativa;
    }

    public String pac() {
        return pac;
    }

    public String terminal() {
        return terminal;
    }

    /** Volta ao estado deslogado e fecha a conexão. */
    public void sair() {
        operador = null;
        nomeOperador = null;
        cooperativa = null;
        pac = null;
        terminal = null;
        conexao.fechar();
    }

    /** Confere a senha do cliente; deve preceder cada operação (as operações não levam a senha). */
    public Resposta validarSenha(String coop, String conta, String senha) throws IOException {
        return conexao.enviar(comConta("validar_senha", coop, conta).adicionar(Rotulo.SENHA_CONTA, senha));
    }

    /**
     * Valida a senha e, só se a validação devolver {@code 000}, executa a operação.
     * Falha de comunicação em qualquer etapa é repassada como {@link IOException}.
     */
    public ResultadoValidado executarComValidacao(String coop, String conta, String senha, OperacaoCliente operacao)
            throws IOException {
        Resposta validacao = validarSenha(coop, conta, senha);
        if (!validacao.sucesso()) {
            return new ResultadoValidado(validacao, null);
        }
        return new ResultadoValidado(validacao, operacao.executar());
    }

    public Resposta saldo(String coop, String conta) throws IOException {
        return conexao.enviar(comConta("saldo", coop, conta));
    }

    public Resposta saque(String coop, String conta, String valorDigitado) throws IOException {
        return conexao.enviar(comConta("saque", coop, conta)
                .adicionar(Rotulo.VALOR, EntradaValor.normalizar(valorDigitado)));
    }

    public Resposta deposito(String coop, String conta, String valorDigitado) throws IOException {
        return conexao.enviar(comConta("deposito", coop, conta)
                .adicionar(Rotulo.VALOR, EntradaValor.normalizar(valorDigitado)));
    }

    public Resposta transferencia(String coop, String conta,
            String coopDestino, String contaDestino, String valorDigitado) throws IOException {
        return conexao.enviar(comConta("transferencia", coop, conta)
                .adicionar(Rotulo.COOPERATIVA_DESTINO, coopDestino.trim())
                .adicionar(Rotulo.CONTA_DESTINO, contaDestino.trim())
                .adicionar(Rotulo.VALOR, EntradaValor.normalizar(valorDigitado)));
    }

    public Resposta extrato(String coop, String conta, String inicioDigitado, String fimDigitado)
            throws IOException {
        Mensagem req = comConta("extrato", coop, conta);
        String inicio = EntradaValor.normalizarData(inicioDigitado);
        String fim = EntradaValor.normalizarData(fimDigitado);
        if (inicio != null) {
            req.adicionar(Rotulo.DATA_INICIO, inicio);
        }
        if (fim != null) {
            req.adicionar(Rotulo.DATA_FIM, fim);
        }
        return conexao.enviar(req);
    }

    /** Agrupa os rótulos 00081–00085 da resposta do extrato, na ordem em que vieram. */
    public static List<LinhaExtrato> linhasExtrato(Mensagem resposta) {
        List<String> datas = resposta.todos(Rotulo.LANC_DATA_HORA.codigo());
        List<String> tipos = resposta.todos(Rotulo.LANC_TIPO.codigo());
        List<String> valores = resposta.todos(Rotulo.LANC_VALOR.codigo());
        List<String> saldos = resposta.todos(Rotulo.LANC_SALDO_APOS.codigo());
        List<String> contrapartes = resposta.todos(Rotulo.LANC_CONTRAPARTE.codigo());
        int n = Math.min(Math.min(datas.size(), tipos.size()),
                Math.min(Math.min(valores.size(), saldos.size()), contrapartes.size()));
        List<LinhaExtrato> linhas = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            linhas.add(new LinhaExtrato(datas.get(i), tipos.get(i), valores.get(i), saldos.get(i), contrapartes.get(i)));
        }
        return linhas;
    }

    private Mensagem comConta(String transacao, String coop, String conta) {
        if (!logado()) {
            throw new IllegalStateException("operador não logado");
        }
        return new Mensagem()
                .adicionar(Rotulo.TRANSACAO, transacao)
                .adicionar(Rotulo.OPERADOR, operador)
                .adicionar(Rotulo.COOPERATIVA_OPERADOR, cooperativa)
                .adicionar(Rotulo.PAC, pac)
                .adicionar(Rotulo.TERMINAL, terminal)
                .adicionar(Rotulo.COOPERATIVA_CONTA, coop.trim())
                .adicionar(Rotulo.CONTA, conta.trim());
    }
}
