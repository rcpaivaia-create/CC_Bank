package br.com.ccbank.cliente;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.slf4j.LoggerFactory;

import br.com.ccbank.cliente.tela.JanelaPrincipal;

/** Abre o cliente com as duas metades. */
public final class ClienteMain {

    private ClienteMain() {
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            LoggerFactory.getLogger(ClienteMain.class).warn("Look and feel do sistema indisponível: {}", e.getMessage());
        }
        SwingUtilities.invokeLater(() -> new JanelaPrincipal().setVisible(true));
    }
}
