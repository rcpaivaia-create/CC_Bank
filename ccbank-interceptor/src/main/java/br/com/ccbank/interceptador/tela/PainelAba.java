package br.com.ccbank.interceptador.tela;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.ccbank.interceptador.config.LadoInterceptador;
import br.com.ccbank.interceptador.historico.Historico;
import br.com.ccbank.interceptador.historico.TrocaRegistrada;
import br.com.ccbank.interceptador.mediacao.ControladorManual;
import br.com.ccbank.interceptador.mediacao.MediadorModo;
import br.com.ccbank.interceptador.mediacao.ModoInterceptador;
import br.com.ccbank.interceptador.mediacao.OuvinteTroca;
import br.com.ccbank.interceptador.proxy.Replay;
import br.com.ccbank.interceptador.proxy.ServidorProxy;
import br.com.ccbank.interceptador.regras.CarregadorRegras;
import br.com.ccbank.protocolo.CodecMensagem;

/**
 * Uma aba do interceptador: seletor de modo, carregar regras, painel manual e histórico com
 * replay. Hospeda um {@link ServidorProxy} do seu lado. A porta só abre em {@link #iniciar()}.
 */
public final class PainelAba extends JPanel {

    private static final long serialVersionUID = 1L;

    private final LadoInterceptador lado;
    private final Historico historico = new Historico();
    private final ControladorManual controladorManual = new ControladorManual(ControladorManual.Filtro.AMBAS);
    private final MediadorModo mediadorModo = new MediadorModo(controladorManual);
    private final Logger log;

    private final ModeloCamposEditaveis modeloCampos = new ModeloCamposEditaveis();
    private final ModeloHistorico modeloHistorico = new ModeloHistorico(historico);
    private final JButton encaminhar = new JButton("Encaminhar");
    private final JButton encaminharOriginal = new JButton("Encaminhar original");
    private final JButton descartar = new JButton("Descartar");
    private final JLabel estado = new JLabel();

    private ServidorProxy proxy;

    public PainelAba(LadoInterceptador lado) {
        super(new BorderLayout(4, 4));
        this.lado = lado;
        this.log = LoggerFactory.getLogger("ccbank.interceptador." + lado.sufixoLog());
        controladorManual.aoMudarRetencao(() -> SwingUtilities.invokeLater(this::atualizarManual));
        add(barraSuperior(), BorderLayout.NORTH);
        add(corpo(), BorderLayout.CENTER);
        atualizarManual();
    }

    /** O ouvinte que a aba liga ao proxy: registra no histórico e atualiza a tabela na EDT. */
    private OuvinteTroca ouvinte() {
        return (sentido, original, decisao) -> {
            historico.observar(sentido, original, decisao);
            SwingUtilities.invokeLater(modeloHistorico::atualizar);
        };
    }

    /** Abre a porta de escuta do lado e começa a interceptar. */
    public void iniciar() throws IOException {
        proxy = ServidorProxy.doLado(lado, mediadorModo, ouvinte());
        proxy.iniciar();
        estado.setText("escutando " + lado.portaEscuta() + " → " + lado.hostDestino() + ":" + lado.portaDestino());
    }

    public void parar() {
        if (proxy != null) {
            proxy.parar();
        }
    }

    public ModoInterceptador modoAtual() {
        return mediadorModo.modo();
    }

    private JComponent barraSuperior() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<ModoInterceptador> modos = new JComboBox<>(ModoInterceptador.values());
        modos.addActionListener(e -> mediadorModo.modo((ModoInterceptador) modos.getSelectedItem()));
        JButton carregar = new JButton("Carregar regras...");
        carregar.addActionListener(e -> carregarRegras());
        barra.add(new JLabel("Modo:"));
        barra.add(modos);
        barra.add(carregar);
        barra.add(estado);
        return barra;
    }

    private JComponent corpo() {
        JSplitPane divisor = new JSplitPane(JSplitPane.VERTICAL_SPLIT, painelManual(), painelHistorico());
        divisor.setResizeWeight(0.5);
        return divisor;
    }

    private JComponent painelManual() {
        JPanel painel = new JPanel(new BorderLayout(4, 4));
        JTable tabela = new JTable(modeloCampos);
        painel.add(new JScrollPane(tabela), BorderLayout.CENTER);

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<ControladorManual.Filtro> filtro = new JComboBox<>(ControladorManual.Filtro.values());
        filtro.setSelectedItem(ControladorManual.Filtro.AMBAS);
        filtro.addActionListener(e -> controladorManual.filtro((ControladorManual.Filtro) filtro.getSelectedItem()));
        JButton adicionar = new JButton("+ linha");
        adicionar.addActionListener(e -> modeloCampos.adicionarLinha());
        JButton remover = new JButton("- linha");
        remover.addActionListener(e -> {
            int linha = tabela.getSelectedRow();
            if (linha >= 0) {
                modeloCampos.removerLinha(linha);
            }
        });
        encaminhar.addActionListener(e -> controladorManual.encaminhar(modeloCampos.montarMensagem()));
        encaminharOriginal.addActionListener(e -> controladorManual.encaminharOriginal());
        descartar.addActionListener(e -> controladorManual.descartar());
        acoes.add(new JLabel("Reter:"));
        acoes.add(filtro);
        acoes.add(adicionar);
        acoes.add(remover);
        acoes.add(encaminhar);
        acoes.add(encaminharOriginal);
        acoes.add(descartar);
        painel.add(acoes, BorderLayout.SOUTH);
        return painel;
    }

    private JComponent painelHistorico() {
        JPanel painel = new JPanel(new BorderLayout(4, 4));
        JTable tabela = new JTable(modeloHistorico);
        painel.add(new JScrollPane(tabela), BorderLayout.CENTER);
        JButton reenviar = new JButton("Reenviar...");
        reenviar.addActionListener(e -> reenviar(tabela.getSelectedRow()));
        JPanel sul = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sul.add(reenviar);
        painel.add(sul, BorderLayout.SOUTH);
        return painel;
    }

    private void atualizarManual() {
        boolean retido = controladorManual.retida().isPresent();
        if (retido) {
            modeloCampos.popular(controladorManual.retida().get());
        }
        encaminhar.setEnabled(retido);
        encaminharOriginal.setEnabled(retido);
        descartar.setEnabled(retido);
    }

    private void carregarRegras() {
        JFileChooser escolha = new JFileChooser();
        if (escolha.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            mediadorModo.carregarRegras(CarregadorRegras.carregar(escolha.getSelectedFile().toPath()));
            log.info("Regras carregadas de {}", escolha.getSelectedFile());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Regras inválidas: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void reenviar(int linhaSelecionada) {
        if (linhaSelecionada < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma troca no histórico.");
            return;
        }
        TrocaRegistrada troca = modeloHistorico.linha(linhaSelecionada);
        String resposta = JOptionPane.showInputDialog(this, "Quantas vezes reenviar?", "1");
        if (resposta == null) {
            return;
        }
        final int vezes;
        try {
            vezes = Integer.parseInt(resposta.trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Número inválido.");
            return;
        }
        byte[] requisicao = CodecMensagem.codificar(troca.resultante());
        new javax.swing.SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                List<byte[]> respostas = Replay.reenviar(requisicao, lado.hostDestino(), lado.portaDestino(), vezes);
                return respostas.size();
            }

            @Override
            protected void done() {
                try {
                    JOptionPane.showMessageDialog(PainelAba.this, "Replay concluído: " + get() + " respostas.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(PainelAba.this, "Falha no replay: " + ex.getMessage(),
                            "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
