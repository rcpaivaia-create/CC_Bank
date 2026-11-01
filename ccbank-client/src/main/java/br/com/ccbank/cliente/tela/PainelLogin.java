package br.com.ccbank.cliente.tela;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/** Login do operador da metade. */
final class PainelLogin extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JTextField usuario = new JTextField(16);
    private final JPasswordField senha = new JPasswordField(16);
    private final JButton entrar = new JButton("Entrar");
    private final JLabel erro = new JLabel(" ");

    PainelLogin(PainelLado lado) {
        super(new GridBagLayout());
        JPanel caixa = new JPanel(new GridBagLayout());
        caixa.setBorder(BorderFactory.createTitledBorder("Login do operador"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        c.gridy = 0;
        caixa.add(new JLabel("Usuário:"), c);
        c.gridx = 1;
        caixa.add(usuario, c);
        c.gridx = 0;
        c.gridy = 1;
        caixa.add(new JLabel("Senha:"), c);
        c.gridx = 1;
        caixa.add(senha, c);
        JButton listar = new JButton("Listar operadores...");
        listar.addActionListener(e -> {
            OperadorEstudo o = SeletorOperador.escolher(this);
            if (o != null) {
                usuario.setText(o.login());
                senha.setText(DadosEstudo.SENHA_OPERADOR);
            }
        });
        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.WEST;
        caixa.add(listar, c);
        c.gridx = 1;
        c.gridy = 2;
        c.anchor = GridBagConstraints.EAST;
        caixa.add(entrar, c);
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        erro.setForeground(new Color(0xB00020));
        caixa.add(erro, c);
        add(caixa);

        Runnable acao = () -> {
            erro.setText(" ");
            lado.entrar(usuario.getText().trim(), new String(senha.getPassword()));
        };
        entrar.addActionListener(e -> acao.run());
        senha.addActionListener(e -> acao.run());
        usuario.addActionListener(e -> senha.requestFocusInWindow());
    }

    void preencherUsuario(String login) {
        usuario.setText(login);
    }

    String usuarioDigitado() {
        return usuario.getText();
    }

    void mostrarErro(String texto) {
        erro.setText(texto);
    }

    void limpar() {
        senha.setText("");
        erro.setText(" ");
    }
}
