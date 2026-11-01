package br.com.ccbank.cliente.tela;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/** Diálogo modal que lista as contas de estudo e devolve a escolhida (ou {@code null}). */
final class SeletorConta {

    private SeletorConta() {
    }

    static ContaEstudo escolher(Component pai) {
        Window dono = SwingUtilities.getWindowAncestor(pai);
        JDialog dialogo = new JDialog(dono, "Contas", JDialog.ModalityType.APPLICATION_MODAL);

        DefaultTableModel modelo = new DefaultTableModel(new Object[] {"Cooperativa", "Conta", "Titular"}, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }
        };
        for (ContaEstudo c : DadosEstudo.CONTAS) {
            modelo.addRow(new Object[] {c.cooperativa(), c.numero(), c.titular()});
        }
        JTable tabela = new JTable(modelo);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelo);
        tabela.setRowSorter(sorter);
        if (modelo.getRowCount() > 0) {
            tabela.setRowSelectionInterval(0, 0);
        }

        JTextField filtro = new JTextField(20);
        filtro.getDocument().addDocumentListener((FiltroDocumento) e ->
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(filtro.getText()))));

        ContaEstudo[] escolhida = new ContaEstudo[1];
        JButton ok = new JButton("Selecionar");
        JButton cancelar = new JButton("Cancelar");
        Runnable confirmar = () -> {
            int linha = tabela.getSelectedRow();
            if (linha >= 0) {
                escolhida[0] = DadosEstudo.CONTAS.get(tabela.convertRowIndexToModel(linha));
                dialogo.dispose();
            }
        };
        ok.addActionListener(e -> confirmar.run());
        cancelar.addActionListener(e -> dialogo.dispose());
        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    confirmar.run();
                }
            }
        });

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(new javax.swing.JLabel("Filtrar:"));
        topo.add(filtro);
        JPanel sul = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sul.add(ok);
        sul.add(cancelar);

        dialogo.setLayout(new BorderLayout());
        dialogo.add(topo, BorderLayout.NORTH);
        dialogo.add(new JScrollPane(tabela), BorderLayout.CENTER);
        dialogo.add(sul, BorderLayout.SOUTH);
        dialogo.setSize(420, 400);
        dialogo.setLocationRelativeTo(dono);
        dialogo.setVisible(true);
        return escolhida[0];
    }
}
