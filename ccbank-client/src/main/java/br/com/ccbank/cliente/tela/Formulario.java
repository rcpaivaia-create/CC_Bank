package br.com.ccbank.cliente.tela;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Grade simples de rótulo + campo usada pelos formulários. */
final class Formulario extends JPanel {

    private static final long serialVersionUID = 1L;

    private int linha;

    Formulario() {
        super(new GridBagLayout());
    }

    void campo(String rotulo, JComponent campo) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = linha;
        add(new JLabel(rotulo), c);
        c.gridx = 1;
        add(campo, c);
        linha++;
    }

    void linhaInteira(JComponent componente) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = linha;
        c.gridwidth = 2;
        add(componente, c);
        linha++;
    }
}
