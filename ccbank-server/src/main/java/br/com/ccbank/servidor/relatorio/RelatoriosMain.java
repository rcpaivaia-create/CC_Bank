package br.com.ccbank.servidor.relatorio;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.ccbank.servidor.config.ConfiguracaoBanco;

/** Abre a ferramenta administrativa de relatórios. Log em {@code logs/admin.log}. */
public final class RelatoriosMain {

    private RelatoriosMain() {
    }

    public static void main(String[] args) {
        // Antes do primeiro uso do SLF4J: o logback.xml usa ccbank.log no nome do arquivo de log.
        System.setProperty("ccbank.log", "admin");
        Logger log = LoggerFactory.getLogger(RelatoriosMain.class);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            log.warn("Look and feel do sistema indisponível: {}", e.getMessage());
        }
        ConfiguracaoBanco cfg;
        try {
            cfg = ConfiguracaoBanco.carregar(ConfiguracaoBanco.arquivoPadrao());
        } catch (Exception e) {
            log.error("Falha ao carregar a configuração do banco: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(null, "Falha ao carregar a configuração do banco:\n" + e.getMessage(),
                    "CC_Bank · Relatórios", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }
        log.info("Ferramenta de relatórios aberta ({})", cfg);
        ConfiguracaoBanco carregada = cfg;
        SwingUtilities.invokeLater(() -> new JanelaRelatorios(carregada).setVisible(true));
    }
}
