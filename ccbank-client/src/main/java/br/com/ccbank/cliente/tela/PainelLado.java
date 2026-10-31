package br.com.ccbank.cliente.tela;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import br.com.ccbank.cliente.ClienteBanco;
import br.com.ccbank.cliente.LadoCliente;
import br.com.ccbank.cliente.Resposta;

/**
 * Uma metade do cliente: cabeçalho (conexão, interceptador, operador), login/operações e o
 * painel de mensagens. A metade esquerda fala com o v1 e a direita com o v2.
 */
public final class PainelLado extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final Color CINZA = new Color(0x9E9E9E);
    private static final Color VERDE = new Color(0x2E7D32);
    private static final Color VERMELHO = new Color(0xB00020);

    private final LadoCliente lado;
    private ClienteBanco cliente;
    private final JCheckBox checkInterceptador = new JCheckBox("Via interceptador");
    private final JLabel status = new JLabel();
    private final JLabel operadorInfo = new JLabel("(não logado)");
    private final JLabel erro = new JLabel(" ");
    private final CardLayout cartoes = new CardLayout();
    private final JPanel centro = new JPanel(cartoes);
    private final PainelLogin painelLogin;
    private final PainelMensagens mensagens;
    private String cartaoAtual = "login";

    public PainelLado(LadoCliente lado) {
        super(new BorderLayout(0, 6));
        this.lado = lado;
        this.cliente = new ClienteBanco(lado, false);
        this.painelLogin = new PainelLogin(this);
        this.mensagens = new PainelMensagens(lado);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, CINZA),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        add(criarCabecalho(), BorderLayout.NORTH);
        centro.add(painelLogin, "login");
        centro.add(criarPainelOperacoes(), "operacoes");
        add(centro, BorderLayout.CENTER);
        add(mensagens, BorderLayout.SOUTH);

        checkInterceptador.addActionListener(e -> trocarRota());
        atualizarCabecalho(CINZA);
    }

    public boolean logado() {
        return cliente.logado();
    }

    public JCheckBox checkInterceptador() {
        return checkInterceptador;
    }

    public String cartaoAtual() {
        return cartaoAtual;
    }

    public ClienteBanco cliente() {
        return cliente;
    }

    public LadoCliente lado() {
        return lado;
    }

    /**
     * Executa uma chamada ao servidor fora da EDT. Desabilita a metade durante a chamada,
     * registra a troca no painel de mensagens e mostra falhas de comunicação na própria metade.
     */
    public void executar(Callable<Resposta> chamada, Consumer<Resposta> aoConcluir) {
        erro.setText(" ");
        habilitar(centro, false);
        checkInterceptador.setEnabled(false);
        Tarefa.executar(chamada,
                r -> {
                    mensagens.registrar(cliente.endereco(), r);
                    atualizarCabecalho(VERDE);
                    aoConcluir.accept(r);
                },
                falha -> {
                    String motivo = falha instanceof IOException || falha instanceof IllegalStateException
                            ? falha.getMessage()
                            : "Erro inesperado: " + falha;
                    mensagens.registrarFalha(cliente.endereco(), motivo);
                    erro.setText(motivo);
                    atualizarCabecalho(VERMELHO);
                },
                () -> {
                    habilitar(centro, true);
                    checkInterceptador.setEnabled(!cliente.logado());
                });
    }

    /** Volta à tela de login; não há sessão no servidor para encerrar. */
    public void sair() {
        cliente.sair();
        painelLogin.limpar();
        mostrar("login");
        atualizarCabecalho(CINZA);
    }

    void entrar(String usuario, String senha) {
        executar(() -> cliente.login(usuario, senha), r -> {
            if (r.sucesso()) {
                mostrar("operacoes");
            } else {
                painelLogin.mostrarErro(r.texto());
            }
            atualizarCabecalho(VERDE);
        });
    }

    /** Painel mostrado depois do login. */
    JPanel criarPainelOperacoes() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton sair = new JButton("Sair");
        sair.addActionListener(e -> sair());
        p.add(sair);
        return p;
    }

    private JPanel criarCabecalho() {
        JPanel cab = new JPanel(new GridLayout(0, 1, 0, 2));
        JLabel titulo = new JLabel(lado.titulo());
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 15f));
        JPanel linhaConexao = new JPanel(new BorderLayout());
        linhaConexao.add(status, BorderLayout.WEST);
        linhaConexao.add(checkInterceptador, BorderLayout.EAST);
        erro.setForeground(VERMELHO);
        cab.add(titulo);
        cab.add(linhaConexao);
        cab.add(operadorInfo);
        cab.add(erro);
        return cab;
    }

    private void trocarRota() {
        if (cliente.logado()) {
            return;
        }
        cliente.sair();
        cliente = new ClienteBanco(lado, checkInterceptador.isSelected());
        erro.setText(" ");
        atualizarCabecalho(CINZA);
    }

    private void mostrar(String cartao) {
        cartoes.show(centro, cartao);
        cartaoAtual = cartao;
    }

    private void atualizarCabecalho(Color corStatus) {
        status.setText("● " + cliente.endereco());
        status.setForeground(corStatus);
        checkInterceptador.setEnabled(!cliente.logado());
        operadorInfo.setText(cliente.logado()
                ? "Operador: " + cliente.operador() + " · Coop " + cliente.cooperativa()
                        + " · PAC " + cliente.pac() + " · Term " + cliente.terminal()
                : "(não logado)");
    }

    private static void habilitar(Container c, boolean habilitado) {
        for (Component filho : c.getComponents()) {
            filho.setEnabled(habilitado);
            if (filho instanceof Container) {
                habilitar((Container) filho, habilitado);
            }
        }
    }
}
