package br.com.ccbank.interceptador.tela;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.table.AbstractTableModel;

import br.com.ccbank.protocolo.Campo;
import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;

/**
 * Tabela editável do modo manual: rótulo | nome (derivado, não editável) | valor. Monta uma
 * {@link Mensagem} a partir das linhas.
 */
public final class ModeloCamposEditaveis extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUNAS = {"Rótulo", "Nome", "Valor"};

    private final List<String[]> linhas = new ArrayList<>(); // [rotulo, valor]

    public void popular(Mensagem m) {
        linhas.clear();
        for (Campo c : m.campos()) {
            linhas.add(new String[] {c.rotulo(), c.valor()});
        }
        fireTableDataChanged();
    }

    public Mensagem montarMensagem() {
        Mensagem m = new Mensagem();
        for (String[] linha : linhas) {
            if (linha[0] != null && !linha[0].trim().isEmpty()) {
                m.adicionar(linha[0], linha[1] == null ? "" : linha[1]);
            }
        }
        return m;
    }

    public void adicionarLinha() {
        linhas.add(new String[] {"", ""});
        fireTableRowsInserted(linhas.size() - 1, linhas.size() - 1);
    }

    public void removerLinha(int i) {
        linhas.remove(i);
        fireTableRowsDeleted(i, i);
    }

    @Override
    public int getRowCount() {
        return linhas.size();
    }

    @Override
    public int getColumnCount() {
        return COLUNAS.length;
    }

    @Override
    public String getColumnName(int coluna) {
        return COLUNAS[coluna];
    }

    @Override
    public Object getValueAt(int linha, int coluna) {
        String[] l = linhas.get(linha);
        switch (coluna) {
            case 0:
                return l[0];
            case 1:
                Optional<Rotulo> r = Rotulo.porCodigo(l[0]);
                return r.isPresent() ? r.get().nome() : "";
            default:
                return l[1];
        }
    }

    @Override
    public boolean isCellEditable(int linha, int coluna) {
        return coluna != 1;
    }

    @Override
    public void setValueAt(Object valor, int linha, int coluna) {
        String texto = valor == null ? "" : valor.toString();
        if (coluna == 0) {
            linhas.get(linha)[0] = texto;
            fireTableRowsUpdated(linha, linha);
        } else if (coluna == 2) {
            linhas.get(linha)[1] = texto;
        }
    }
}
