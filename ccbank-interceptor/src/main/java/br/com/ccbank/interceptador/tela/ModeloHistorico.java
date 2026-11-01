package br.com.ccbank.interceptador.tela;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import br.com.ccbank.interceptador.historico.Historico;
import br.com.ccbank.interceptador.historico.TrocaRegistrada;
import br.com.ccbank.interceptador.mediacao.DecisaoMediacao;

/** Modelo de tabela do histórico de uma aba: hora | sentido | transação | estado. */
public final class ModeloHistorico extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUNAS = {"Hora", "Sentido", "Transação", "Estado"};
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final Historico historico;
    private List<TrocaRegistrada> instantaneo = new ArrayList<>();

    public ModeloHistorico(Historico historico) {
        this.historico = historico;
    }

    /** Relê o histórico e avisa a tabela. */
    public void atualizar() {
        instantaneo = historico.trocas();
        fireTableDataChanged();
    }

    public TrocaRegistrada linha(int i) {
        return instantaneo.get(i);
    }

    @Override
    public int getRowCount() {
        return instantaneo.size();
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
        TrocaRegistrada t = instantaneo.get(linha);
        switch (coluna) {
            case 0:
                return t.hora().format(HORA);
            case 1:
                return t.sentido().name();
            case 2:
                return t.transacao();
            default:
                return estado(t);
        }
    }

    private static String estado(TrocaRegistrada t) {
        if (t.acao() == DecisaoMediacao.Acao.DESCARTAR) {
            return "descartada";
        }
        return t.adulterada() ? "adulterada" : "encaminhada";
    }
}
