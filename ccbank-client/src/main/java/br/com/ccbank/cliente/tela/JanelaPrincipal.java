package br.com.ccbank.cliente.tela;

import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;

import br.com.ccbank.cliente.LadoCliente;

/** Janela do cliente: metade esquerda (v1) e metade direita (v2), lado a lado. */
public final class JanelaPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;

    public JanelaPrincipal() {
        super("CC_Bank");
        PainelLado esquerdo = new PainelLado(LadoCliente.ESQUERDO);
        PainelLado direito = new PainelLado(LadoCliente.DIREITO);
        JPanel metades = new JPanel(new GridLayout(1, 2));
        metades.add(esquerdo);
        metades.add(direito);
        setContentPane(metades);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                esquerdo.cliente().sair();
                direito.cliente().sair();
            }
        });
        setSize(1280, 800);
        setLocationRelativeTo(null);
    }
}
