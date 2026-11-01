package br.com.ccbank.cliente.tela;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import br.com.ccbank.cliente.ClienteBanco;
import br.com.ccbank.cliente.LinhaExtrato;
import br.com.ccbank.cliente.ResultadoValidado;
import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;

/** Extrato por período, em tabela. */
final class PainelExtrato extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUNAS = {"Data/hora", "Descrição", "Valor", "Saldo após", "Contraparte"};

    private final PainelLado lado;
    private final JTextField cooperativa = new JTextField(5);
    private final JTextField conta = new JTextField(8);
    private final JTextField inicio = new JTextField(10);
    private final JTextField fim = new JTextField(10);
    private final JPasswordField senha = new JPasswordField(8);
    private final JLabel situacao = new JLabel(" ");
    private final DefaultTableModel modelo = new DefaultTableModel(COLUNAS, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int linha, int coluna) {
            return false;
        }
    };

    PainelExtrato(PainelLado lado) {
        super(new BorderLayout(0, 6));
        this.lado = lado;
        java.time.LocalDate hoje = java.time.LocalDate.now();
        inicio.setText(DatasExtrato.inicio(hoje));
        fim.setText(DatasExtrato.fim(hoje));
        Formulario f = new Formulario();
        f.campo("Cooperativa:", cooperativa);
        f.campo("Conta:", comBotaoListar());
        f.campo("Data início (DD/MM/AAAA):", inicio);
        f.campo("Data fim (DD/MM/AAAA):", fim);
        f.campo("Senha do cliente:", senha);
        JButton executar = new JButton("Executar");
        executar.addActionListener(e -> executar());
        senha.addActionListener(e -> executar());
        f.linhaInteira(executar);
        f.linhaInteira(situacao);
        add(f, BorderLayout.NORTH);
        add(new JScrollPane(new JTable(modelo)), BorderLayout.CENTER);
    }

    private JPanel comBotaoListar() {
        JPanel painel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        JButton listar = new JButton("Listar...");
        listar.addActionListener(e -> {
            ContaEstudo c = SeletorConta.escolher(this);
            if (c != null) {
                cooperativa.setText(c.cooperativa());
                conta.setText(c.numero());
                senha.setText(DadosEstudo.SENHA_CONTA);
            }
        });
        painel.add(conta);
        painel.add(listar);
        return painel;
    }

    private void executar() {
        String coop = cooperativa.getText();
        String numero = conta.getText();
        String senhaCliente = new String(senha.getPassword());
        String dataInicio = inicio.getText();
        String dataFim = fim.getText();
        senha.setText("");
        situacao.setText(" ");
        modelo.setRowCount(0);
        ClienteBanco c = lado.cliente();
        lado.executar(() -> c.executarComValidacao(coop, numero, senhaCliente,
                () -> c.extrato(coop, numero, dataInicio, dataFim)), ResultadoValidado::respostas, this::mostrar);
    }

    private void mostrar(ResultadoValidado resultado) {
        if (!resultado.sucesso()) {
            situacao.setText(TextoResultado.situacao(resultado));
            situacao.setForeground(new Color(0xB00020));
            return;
        }
        Mensagem r = resultado.operacao().mensagem();
        List<LinhaExtrato> linhas = ClienteBanco.linhasExtrato(r);
        for (LinhaExtrato l : linhas) {
            modelo.addRow(new Object[] {l.dataHora().replace('T', ' '), l.tipo(), l.valor(), l.saldoApos(), l.contraparte()});
        }
        String titular = r.primeiro(Rotulo.TITULAR).orElse("");
        situacao.setText("✔ " + titular + " · " + linhas.size() + " lançamento(s) · " + TextoResultado.tempos(resultado));
        situacao.setForeground(new Color(0x2E7D32));
    }
}
