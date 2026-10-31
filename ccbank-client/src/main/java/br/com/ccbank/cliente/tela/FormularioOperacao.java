package br.com.ccbank.cliente.tela;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import br.com.ccbank.cliente.ClienteBanco;
import br.com.ccbank.cliente.Resposta;
import br.com.ccbank.cliente.ResultadoValidado;
import br.com.ccbank.protocolo.Rotulo;

/** Formulário de saldo, saque, depósito ou transferência, com a área de resultado. */
final class FormularioOperacao extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final Color VERDE = new Color(0x2E7D32);
    private static final Color VERMELHO = new Color(0xB00020);

    enum Tipo {
        SALDO(false, false), SAQUE(true, false), DEPOSITO(true, false), TRANSFERENCIA(true, true);

        final boolean temValor;
        final boolean temDestino;

        Tipo(boolean temValor, boolean temDestino) {
            this.temValor = temValor;
            this.temDestino = temDestino;
        }
    }

    private final PainelLado lado;
    private final Tipo tipo;
    private final JTextField cooperativa = new JTextField(5);
    private final JTextField conta = new JTextField(8);
    private final JTextField valor = new JTextField(12);
    private final JTextField cooperativaDestino = new JTextField(5);
    private final JTextField contaDestino = new JTextField(8);
    private final JPasswordField senha = new JPasswordField(8);
    private final JLabel situacao = new JLabel(" ");
    private final JLabel titular = new JLabel(" ");
    private final JLabel saldo = new JLabel(" ");
    private final JLabel transacao = new JLabel(" ");

    FormularioOperacao(PainelLado lado, Tipo tipo) {
        super(new BorderLayout());
        this.lado = lado;
        this.tipo = tipo;
        Formulario f = new Formulario();
        f.campo("Cooperativa:", cooperativa);
        f.campo("Conta:", conta);
        if (tipo.temValor) {
            f.campo("Valor:", valor);
        }
        if (tipo.temDestino) {
            f.campo("Cooperativa destino:", cooperativaDestino);
            f.campo("Conta destino:", contaDestino);
        }
        f.campo("Senha do cliente:", senha);
        JButton executar = new JButton("Executar");
        executar.addActionListener(e -> executar());
        senha.addActionListener(e -> executar());
        f.linhaInteira(executar);

        JPanel resultado = new JPanel(new GridLayout(0, 1, 0, 2));
        resultado.add(situacao);
        resultado.add(titular);
        resultado.add(saldo);
        resultado.add(transacao);
        f.linhaInteira(resultado);
        add(f, BorderLayout.NORTH);
    }

    private void executar() {
        String coop = cooperativa.getText();
        String numero = conta.getText();
        String senhaCliente = new String(senha.getPassword());
        String valorDigitado = valor.getText();
        String coopDest = cooperativaDestino.getText();
        String contaDest = contaDestino.getText();
        senha.setText("");
        limparResultado();
        ClienteBanco c = lado.cliente();
        lado.executar(() -> c.executarComValidacao(coop, numero, senhaCliente, () -> {
            switch (tipo) {
                case SAQUE:
                    return c.saque(coop, numero, valorDigitado);
                case DEPOSITO:
                    return c.deposito(coop, numero, valorDigitado);
                case TRANSFERENCIA:
                    return c.transferencia(coop, numero, coopDest, contaDest, valorDigitado);
                default:
                    return c.saldo(coop, numero);
            }
        }), ResultadoValidado::respostas, this::mostrar);
    }

    private void mostrar(ResultadoValidado resultado) {
        situacao.setText(TextoResultado.situacao(resultado));
        situacao.setForeground(resultado.sucesso() ? VERDE : VERMELHO);
        Resposta r = resultado.resultadoFinal();
        r.mensagem().primeiro(Rotulo.TITULAR).ifPresent(t -> titular.setText("Titular: " + t));
        r.mensagem().primeiro(Rotulo.SALDO).ifPresent(s -> saldo.setText("Saldo: " + s));
        String id = r.mensagem().primeiro(Rotulo.ID_TRANSACAO).map(i -> "Transação " + i + " · ").orElse("");
        transacao.setText(id + TextoResultado.tempos(resultado));
    }

    private void limparResultado() {
        situacao.setText(" ");
        titular.setText(" ");
        saldo.setText(" ");
        transacao.setText(" ");
    }
}
