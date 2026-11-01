package br.com.ccbank.interceptador.tela;

import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.slf4j.LoggerFactory;

import br.com.ccbank.interceptador.config.LadoInterceptador;

/** Abre a tela do interceptador com as duas abas (Esquerdo 8001→9001, Direito 8002→9002). */
public final class InterceptadorTelaMain {

    private InterceptadorTelaMain() {
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            LoggerFactory.getLogger(InterceptadorTelaMain.class)
                    .warn("Look and feel do sistema indisponível: {}", e.getMessage());
        }
        SwingUtilities.invokeLater(InterceptadorTelaMain::abrir);
    }

    private static void abrir() {
        JFrame janela = new JFrame("CC_Bank — Interceptador");
        janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        janela.setSize(1280, 800);

        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Esquerdo", iniciarAba(janela, LadoInterceptador.ESQUERDO));
        abas.addTab("Direito", iniciarAba(janela, LadoInterceptador.DIREITO));
        janela.setContentPane(abas);
        janela.setLocationRelativeTo(null);
        janela.setVisible(true);
    }

    private static PainelAba iniciarAba(JFrame janela, LadoInterceptador lado) {
        PainelAba aba = new PainelAba(lado);
        try {
            aba.iniciar();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(janela,
                    "Não foi possível abrir a porta " + lado.portaEscuta() + ": " + e.getMessage(),
                    "Erro no lado " + lado.sufixoLog(), JOptionPane.ERROR_MESSAGE);
        }
        return aba;
    }
}
