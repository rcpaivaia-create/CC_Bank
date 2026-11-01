package br.com.ccbank.cliente.tela;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/** Diálogo modal que lista os operadores de estudo e devolve o escolhido (ou {@code null}). */
final class SeletorOperador {

    private SeletorOperador() {
    }

    static OperadorEstudo escolher(Component pai) {
        Window dono = SwingUtilities.getWindowAncestor(pai);
        JDialog dialogo = new JDialog(dono, "Operadores", JDialog.ModalityType.APPLICATION_MODAL);

        DefaultTableModel modelo = new DefaultTableModel(new Object[] {"Login", "Nome"}, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }
        };
        for (OperadorEstudo o : DadosEstudo.OPERADORES) {
            modelo.addRow(new Object[] {o.login(), o.nome()});
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

        OperadorEstudo[] escolhido = new OperadorEstudo[1];
        JButton ok = new JButton("Selecionar");
        JButton cancelar = new JButton("Cancelar");
        Runnable confirmar = () -> {
            int linha = tabela.getSelectedRow();
            if (linha >= 0) {
                escolhido[0] = DadosEstudo.OPERADORES.get(tabela.convertRowIndexToModel(linha));
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
        topo.add(new JLabel("Filtrar:"));
        topo.add(filtro);
        JPanel sul = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sul.add(ok);
        sul.add(cancelar);

        dialogo.setLayout(new BorderLayout());
        dialogo.add(topo, BorderLayout.NORTH);
        dialogo.add(new JScrollPane(tabela), BorderLayout.CENTER);
        dialogo.add(sul, BorderLayout.SOUTH);
        dialogo.setSize(360, 360);
        dialogo.setLocationRelativeTo(dono);
        dialogo.setVisible(true);
        return escolhido[0];
    }
}
