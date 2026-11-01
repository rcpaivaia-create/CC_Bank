package br.com.ccbank.interceptador.proxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.ccbank.interceptador.regras.MotorRegras;
import br.com.ccbank.interceptador.regras.ResultadoAplicacao;
import br.com.ccbank.interceptador.regras.Sentido;
import br.com.ccbank.protocolo.CodecMensagem;
import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.MensagemMalformadaException;
import br.com.ccbank.protocolo.MensagemMuitoGrandeException;

/**
 * Trata um par cliente↔servidor do proxy. O protocolo é síncrono (uma mensagem em voo), então
 * o loop lê um quadro do cliente, adultera no sentido {@link Sentido#REQUISICAO} e repassa ao
 * servidor; depois lê a resposta, adultera no sentido {@link Sentido#RESPOSTA} e devolve ao
 * cliente. Quadro que não decodifica passa em modo opaco (bytes intactos); quadro acima do
 * limite de 64 KB derruba o par.
 */
final class ParDeConexoes {

    private static final Logger LOG = LoggerFactory.getLogger(ParDeConexoes.class);
    private static final int TIMEOUT_CONEXAO_MS = 5_000;

    private final Socket cliente;
    private final String hostDestino;
    private final int portaDestino;
    private final MotorRegras motor;
    private final String sufixoLog;

    ParDeConexoes(Socket cliente, String hostDestino, int portaDestino, MotorRegras motor, String sufixoLog) {
        this.cliente = cliente;
        this.hostDestino = hostDestino;
        this.portaDestino = portaDestino;
        this.motor = motor;
        this.sufixoLog = sufixoLog;
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
                    repassar(requisicao, Sentido.REQUISICAO, outServidor, "recebida do cliente", "enviada ao servidor");
                    byte[] resposta = lerQuadro(inServidor, hostDestino + ":" + portaDestino);
                    if (resposta == null) {
                        break;
                    }
                    repassar(resposta, Sentido.RESPOSTA, outCliente, "recebida do servidor", "devolvida ao cliente");
                }
            }
        } catch (IOException e) {
            LOG.warn("[{}] par {} encerrado: {}", sufixoLog, enderecoCliente, e.getMessage());
        } finally {
            fechar(cliente);
        }
    }

    /** @return o quadro, {@code null} em fim de stream, derrubando o par se exceder 64 KB */
    private byte[] lerQuadro(InputStream in, String origem) throws IOException {
        try {
            return CodecMensagem.lerQuadro(in);
        } catch (MensagemMuitoGrandeException e) {
            LOG.warn("[{}] quadro de {} acima de 64 KB; par derrubado", sufixoLog, origem);
            return null;
        }
    }

    private void repassar(byte[] quadro, Sentido sentido, OutputStream destino, String etapaEntrada, String etapaSaida)
            throws IOException {
        Mensagem mensagem;
        try {
            mensagem = CodecMensagem.decodificar(quadro);
        } catch (MensagemMalformadaException e) {
            LOG.info("[{}] {} (opaco, {} bytes): {}", sufixoLog, etapaEntrada, quadro.length, hex(quadro));
            destino.write(quadro);
            destino.flush();
            return;
        }
        LOG.info("[{}] {}\n{}", sufixoLog, etapaEntrada, br.com.ccbank.protocolo.FormatadorLog.formatarCampos(mensagem));
        ResultadoAplicacao resultado = motor.aplicar(mensagem, sentido);
        LOG.info("[{}] {}\n{}", sufixoLog, etapaSaida,
                FormatadorAdulteracao.formatar(resultado.mensagem(), resultado.adulteracoes()));
        byte[] saida = resultado.houveAdulteracao() ? CodecMensagem.codificar(resultado.mensagem()) : quadro;
        destino.write(saida);
        destino.flush();
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
            LOG.debug("[{}] erro ao fechar socket do cliente", sufixoLog, e);
        }
    }
}
