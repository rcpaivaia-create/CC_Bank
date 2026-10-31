package br.com.ccbank.cliente.tela;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;

/** Barra de operações e o formulário da operação escolhida. */
public final class PainelOperacoes extends JPanel {

    private static final long serialVersionUID = 1L;

    private final CardLayout cartoes = new CardLayout();
    private final JPanel formularios = new JPanel(cartoes);
    private final Map<String, JButton> botoes = new LinkedHashMap<>();
    private String formularioAtual = "saldo";

    public PainelOperacoes(PainelLado lado) {
        super(new BorderLayout(0, 6));
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        adicionar(barra, "Saldo", "saldo", new FormularioOperacao(lado, FormularioOperacao.Tipo.SALDO));
        adicionar(barra, "Saque", "saque", new FormularioOperacao(lado, FormularioOperacao.Tipo.SAQUE));
        adicionar(barra, "Depósito", "deposito", new FormularioOperacao(lado, FormularioOperacao.Tipo.DEPOSITO));
        adicionar(barra, "Transferência", "transferencia",
                new FormularioOperacao(lado, FormularioOperacao.Tipo.TRANSFERENCIA));
        adicionar(barra, "Extrato", "extrato", new PainelExtrato(lado));
        JButton sair = new JButton("Sair");
        sair.addActionListener(e -> {
            mostrar("saldo");
            lado.sair();
        });
        botoes.put("Sair", sair);
        barra.add(sair);
        add(barra, BorderLayout.NORTH);
        add(formularios, BorderLayout.CENTER);
    }

    public String formularioAtual() {
        return formularioAtual;
    }

    public JButton botao(String rotulo) {
        return botoes.get(rotulo);
    }

    private void adicionar(JPanel barra, String rotulo, String cartao, JPanel formulario) {
        formularios.add(formulario, cartao);
        JButton b = new JButton(rotulo);
        b.addActionListener(e -> mostrar(cartao));
        botoes.put(rotulo, b);
        barra.add(b);
    }

    private void mostrar(String cartao) {
        cartoes.show(formularios, cartao);
        formularioAtual = cartao;
    }
}
