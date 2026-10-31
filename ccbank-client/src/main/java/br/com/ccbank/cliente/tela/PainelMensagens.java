package br.com.ccbank.cliente.tela;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import br.com.ccbank.cliente.LadoCliente;
import br.com.ccbank.cliente.Resposta;
import br.com.ccbank.protocolo.Direcao;
import br.com.ccbank.protocolo.FormatadorLog;

/** Painel recolhível com as mensagens trocadas pela metade, com os nomes legíveis dos rótulos. */
final class PainelMensagens extends JPanel {

    private static final long serialVersionUID = 1L;

    private final LadoCliente lado;
    private final JTextArea texto = new JTextArea();
    private final JScrollPane rolagem = new JScrollPane(texto);
    private final JButton alternar = new JButton("▸ Mensagens");

    PainelMensagens(LadoCliente lado) {
        super(new BorderLayout());
        this.lado = lado;
        texto.setEditable(false);
        texto.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        rolagem.setPreferredSize(new Dimension(100, 220));
        rolagem.setVisible(false);
        alternar.setHorizontalAlignment(JButton.LEFT);
        alternar.addActionListener(e -> {
            boolean mostrar = !rolagem.isVisible();
            rolagem.setVisible(mostrar);
            alternar.setText((mostrar ? "▾" : "▸") + " Mensagens");
            revalidate();
        });
        add(alternar, BorderLayout.NORTH);
        add(rolagem, BorderLayout.CENTER);
    }

    void registrar(String endereco, Resposta r) {
        acrescentar(FormatadorLog.formatarTroca(lado.sigla(), Direcao.ENVIADA, endereco, r.requisicao(), null));
        acrescentar(FormatadorLog.formatarTroca(lado.sigla(), Direcao.RECEBIDA, endereco, r.mensagem(), r.duracaoMs()));
    }

    void registrarFalha(String endereco, String motivo) {
        acrescentar("[" + lado.sigla() + "] FALHA " + endereco + ": " + motivo);
    }

    private void acrescentar(String linha) {
        texto.append(linha + "\n\n");
        texto.setCaretPosition(texto.getDocument().getLength());
    }
}
