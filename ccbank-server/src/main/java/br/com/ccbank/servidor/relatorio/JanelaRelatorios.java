package br.com.ccbank.servidor.relatorio;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.sql.DataSource;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.ccbank.servidor.config.Ambiente;
import br.com.ccbank.servidor.config.Caminhos;
import br.com.ccbank.servidor.config.ConfiguracaoBanco;

/** Ferramenta administrativa: filtra o jornal de um ambiente e gera o relatório HTML. */
public final class JanelaRelatorios extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(JanelaRelatorios.class);
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATA_BR =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final String[] OPERACOES =
            {"Todas", "LOGIN", "SALDO", "SAQUE", "DEPOSITO", "TRANSFERENCIA", "EXTRATO", "INVALIDA"};

    private final ConfiguracaoBanco cfg;
    private final Map<Ambiente, DataSource> pools = new EnumMap<>(Ambiente.class);

    private final JComboBox<String> ambiente = new JComboBox<>(new String[] {"v1", "v2"});
    private final JTextField inicio = new JTextField(10);
    private final JTextField fim = new JTextField(10);
    private final JTextField cooperativa = new JTextField(6);
    private final JTextField pac = new JTextField(6);
    private final JTextField terminal = new JTextField(6);
    private final JTextField operador = new JTextField(14);
    private final JComboBox<String> operacao = new JComboBox<>(OPERACOES);
    private final JCheckBox somenteIntercredis = new JCheckBox("Só Intercredis");
    private final JCheckBox somenteDivergencias = new JCheckBox("Só divergências de origem");
    private final JCheckBox incluirRecusadas = new JCheckBox("Incluir recusadas", true);
    private final JButton gerar = new JButton("Gerar");
    private final JLabel situacao = new JLabel(" ");

    public JanelaRelatorios(ConfiguracaoBanco cfg) {
        super("CC_Bank · Relatórios");
        this.cfg = cfg;
        String hoje = DATA_BR.format(LocalDate.now(FUSO));
        inicio.setText(hoje);
        fim.setText(hoje);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        int linha = 0;
        linha = campo(form, linha, "Ambiente:", ambiente);
        linha = campo(form, linha, "Data início (DD/MM/AAAA):", inicio);
        linha = campo(form, linha, "Data fim (DD/MM/AAAA):", fim);
        linha = campo(form, linha, "Cooperativa:", cooperativa);
        linha = campo(form, linha, "PAC:", pac);
        linha = campo(form, linha, "Terminal:", terminal);
        linha = campo(form, linha, "Operador:", operador);
        linha = campo(form, linha, "Operação:", operacao);
        linha = campo(form, linha, "", somenteIntercredis);
        linha = campo(form, linha, "", somenteDivergencias);
        linha = campo(form, linha, "", incluirRecusadas);
        campo(form, linha, "", gerar);

        situacao.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(situacao, BorderLayout.SOUTH);
        gerar.addActionListener(e -> gerar());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(640, 480);
        setLocationRelativeTo(null);
    }

    private static int campo(JPanel form, int linha, String rotulo, JComponent campo) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = linha;
        form.add(new JLabel(rotulo), c);
        c.gridx = 1;
        form.add(campo, c);
        return linha + 1;
    }

    private void gerar() {
        LocalDate dataInicio;
        LocalDate dataFim;
        try {
            dataInicio = LocalDate.parse(inicio.getText().trim(), DATA_BR);
            dataFim = LocalDate.parse(fim.getText().trim(), DATA_BR);
        } catch (DateTimeParseException e) {
            mostrar("Data inválida: use DD/MM/AAAA.", true);
            return;
        }
        if (dataInicio.isAfter(dataFim)) {
            mostrar("A data de início é posterior à data de fim.", true);
            return;
        }
        String nomeAmbiente = (String) ambiente.getSelectedItem();
        Ambiente amb = Ambiente.porNome(nomeAmbiente);
        String tipo = operacao.getSelectedIndex() == 0 ? null : (String) operacao.getSelectedItem();
        FiltroRelatorio filtro = new FiltroRelatorio(dataInicio, dataFim)
                .cooperativa(cooperativa.getText())
                .pac(pac.getText())
                .terminal(terminal.getText())
                .operador(operador.getText())
                .tipo(tipo)
                .somenteIntercredis(somenteIntercredis.isSelected())
                .somenteDivergencias(somenteDivergencias.isSelected())
                .incluirRecusadas(incluirRecusadas.isSelected());

        gerar.setEnabled(false);
        mostrar("Gerando...", false);
        new SwingWorker<Path, Void>() {
            private int quantidade;

            @Override
            protected Path doInBackground() throws Exception {
                List<LinhaRelatorio> linhas = new ConsultaRelatorio(pool(amb)).consultar(filtro);
                quantidade = linhas.size();
                LocalDateTime agora = LocalDateTime.now(FUSO);
                String html = GeradorRelatorioHtml.gerar(nomeAmbiente, filtro, linhas, agora);
                return ArquivoRelatorio.salvar(Caminhos.localizar("relatorios"),
                        ArquivoRelatorio.nome(nomeAmbiente.toLowerCase(Locale.ROOT), agora), html);
            }

            @Override
            protected void done() {
                gerar.setEnabled(true);
                try {
                    Path arquivo = get();
                    LOG.info("Relatório {} gerado: período {} a {}, {} linha(s), arquivo {}",
                            nomeAmbiente, dataInicio, dataFim, quantidade, arquivo);
                    mostrar("Relatório gerado (" + quantidade + " linha(s)): " + arquivo, false);
                    abrir(arquivo);
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause() != null ? e.getCause() : e;
                    LOG.error("Falha ao gerar relatório {}", nomeAmbiente, causa);
                    mostrar("Falha ao gerar: " + causa.getMessage(), true);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private synchronized DataSource pool(Ambiente amb) {
        return pools.computeIfAbsent(amb, a -> cfg.criarPool(a.banco(), 2));
    }

    private void abrir(Path arquivo) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(arquivo.toUri());
            }
        } catch (Exception e) {
            LOG.warn("Não foi possível abrir o navegador: {}", e.getMessage());
            mostrar("Relatório gerado em " + arquivo + " (abra manualmente no navegador)", false);
        }
    }

    private void mostrar(String texto, boolean erro) {
        situacao.setText(texto);
        situacao.setForeground(erro ? new Color(0xB00020) : new Color(0x2E7D32));
    }
}
