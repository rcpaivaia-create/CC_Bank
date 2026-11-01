package br.com.ccbank.interceptador.proxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.ccbank.interceptador.mediacao.DecisaoMediacao;
import br.com.ccbank.interceptador.mediacao.Mediador;
import br.com.ccbank.interceptador.mediacao.OuvinteTroca;
import br.com.ccbank.protocolo.CodecMensagem;
import br.com.ccbank.protocolo.FormatadorLog;
import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.MensagemMalformadaException;
import br.com.ccbank.protocolo.MensagemMuitoGrandeException;
import br.com.ccbank.interceptador.regras.Sentido;

/**
 * Trata um par cliente↔servidor do proxy. O protocolo é síncrono (uma mensagem em voo), então
 * o loop lê um quadro do cliente, consulta o {@link Mediador} no sentido {@link Sentido#REQUISICAO}
 * e repassa ao servidor; depois lê a resposta, consulta no sentido {@link Sentido#RESPOSTA} e
 * devolve ao cliente. Quadro que não decodifica passa em modo opaco (bytes intactos, sem mediar);
 * quadro acima de 64 KB derruba o par. Uma mensagem descartada não é escrita.
 */
final class ParDeConexoes {

    private static final int TIMEOUT_CONEXAO_MS = 5_000;

    private final Socket cliente;
    private final String hostDestino;
    private final int portaDestino;
    private final Mediador mediador;
    private final OuvinteTroca ouvinte;
    private final String sufixoLog;
    private final Set<Socket> registro;
    private final Logger log;

    ParDeConexoes(Socket cliente, String hostDestino, int portaDestino, Mediador mediador, OuvinteTroca ouvinte,
            String sufixoLog, Set<Socket> registro) {
        this.cliente = cliente;
        this.hostDestino = hostDestino;
        this.portaDestino = portaDestino;
        this.mediador = mediador;
        this.ouvinte = ouvinte;
        this.sufixoLog = sufixoLog;
        this.registro = registro;
        this.log = LoggerFactory.getLogger("ccbank.interceptador." + sufixoLog);
    }

    void executar() {
        String enderecoCliente = cliente.getInetAddress().getHostAddress() + ":" + cliente.getPort();
        try (Socket servidor = new Socket()) {
            servidor.connect(new InetSocketAddress(hostDestino, portaDestino), TIMEOUT_CONEXAO_MS);
            try (InputStream inCliente = new BufferedInputStream(cliente.getInputStream());
                    OutputStream outCliente = new BufferedOutputStream(cliente.getOutputStream());
                    InputStream inServidor = new BufferedInputStream(servidor.getInputStream());
                    OutputStream outServidor = new BufferedOutputStream(servidor.getOutputStream())) {
                while (true) {
                    byte[] requisicao = lerQuadro(inCliente, enderecoCliente);
                    if (requisicao == null) {
                        break;
                    }
                    if (!repassar(requisicao, Sentido.REQUISICAO, outServidor, "recebida do cliente",
                            "enviada ao servidor")) {
                        continue; // requisição descartada: não há round-trip ao servidor
                    }
                    byte[] resposta = lerQuadro(inServidor, hostDestino + ":" + portaDestino);
                    if (resposta == null) {
                        break;
                    }
                    repassar(resposta, Sentido.RESPOSTA, outCliente, "recebida do servidor", "devolvida ao cliente");
                }
            }
        } catch (IOException e) {
            log.warn("[{}] par {} encerrado: {}", sufixoLog, enderecoCliente, e.getMessage());
        } finally {
            registro.remove(cliente);
            fechar(cliente);
        }
    }

    /** @return o quadro, {@code null} em fim de stream, derrubando o par se exceder 64 KB */
    private byte[] lerQuadro(InputStream in, String origem) throws IOException {
        try {
            return CodecMensagem.lerQuadro(in);
        } catch (MensagemMuitoGrandeException e) {
            log.warn("[{}] quadro de {} acima de 64 KB; par derrubado", sufixoLog, origem);
            return null;
        }
    }

    /**
     * Repassa um quadro à {@code destino} conforme o mediador.
     *
     * @return {@code true} se encaminhou (ou passou opaco); {@code false} se descartou
     */
    private boolean repassar(byte[] quadro, Sentido sentido, OutputStream destino, String etapaEntrada,
            String etapaSaida) throws IOException {
        Mensagem mensagem;
        try {
            mensagem = CodecMensagem.decodificar(quadro);
        } catch (MensagemMalformadaException e) {
            log.info("[{}] {} (opaco, {} bytes): {}", sufixoLog, etapaEntrada, quadro.length, hex(quadro));
            destino.write(quadro);
            destino.flush();
            return true;
        }
        log.info("[{}] {}\n{}", sufixoLog, etapaEntrada, FormatadorLog.formatarCampos(mensagem));
        DecisaoMediacao decisao = mediador.intervir(mensagem, sentido);
        ouvinte.observar(sentido, mensagem, decisao);
        if (decisao.acao() == DecisaoMediacao.Acao.DESCARTAR) {
            log.info("[{}] {} DESCARTADA", sufixoLog, etapaSaida);
            return false;
        }
        log.info("[{}] {}\n{}", sufixoLog, etapaSaida,
                FormatadorAdulteracao.formatar(decisao.resultado().mensagem(), decisao.resultado().adulteracoes()));
        byte[] saida = decisao.reserializar() ? CodecMensagem.codificar(decisao.resultado().mensagem()) : quadro;
        destino.write(saida);
        destino.flush();
        return true;
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void fechar(Socket s) {
        try {
            s.close();
        } catch (IOException e) {
            log.debug("[{}] erro ao fechar socket do cliente", sufixoLog, e);
        }
    }
}
