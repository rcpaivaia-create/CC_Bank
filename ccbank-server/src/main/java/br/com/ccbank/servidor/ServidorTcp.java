package br.com.ccbank.servidor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.ccbank.protocolo.CodecMensagem;
import br.com.ccbank.protocolo.CodigoRetorno;
import br.com.ccbank.protocolo.Direcao;
import br.com.ccbank.protocolo.FormatadorLog;
import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.MensagemMalformadaException;
import br.com.ccbank.protocolo.MensagemMuitoGrandeException;
import br.com.ccbank.protocolo.Rotulo;
import br.com.ccbank.servidor.negocio.ProcessadorTransacoes;

/**
 * Servidor TCP do protocolo: uma thread por conexão, requisição/resposta síncrona.
 *
 * <p>Mensagem malformada recebe {@code 006} e a conexão continua. Mensagem acima do limite
 * ou fim do stream no meio de uma mensagem encerram a conexão.
 */
public final class ServidorTcp {

    private static final Logger LOG = LoggerFactory.getLogger(ServidorTcp.class);
    private static final Logger MENSAGENS = LoggerFactory.getLogger("ccbank.mensagens");

    private final int porta;
    private final ProcessadorTransacoes processador;
    private final String lado;
    private final Set<Socket> abertas = ConcurrentHashMap.newKeySet();
    private final AtomicInteger contador = new AtomicInteger();
    private final ExecutorService conexoes;
    private volatile ServerSocket serverSocket;

    public ServidorTcp(int porta, ProcessadorTransacoes processador, String lado) {
        this.porta = porta;
        this.processador = processador;
        this.lado = lado;
        this.conexoes = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "conexao-" + lado + "-" + contador.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    /** Abre a porta e passa a aceitar conexões numa thread própria; não bloqueia. */
    public void iniciar() throws IOException {
        serverSocket = new ServerSocket(porta);
        Thread aceitador = new Thread(this::aceitar, "aceitador-" + lado);
        aceitador.start();
    }

    public int portaLocal() {
        return serverSocket.getLocalPort();
    }

    public void parar() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            LOG.debug("Erro ao fechar a porta", e);
        }
        for (Socket s : abertas) {
            fechar(s);
        }
        conexoes.shutdownNow();
    }

    private void aceitar() {
        while (!serverSocket.isClosed()) {
            try {
                Socket s = serverSocket.accept();
                abertas.add(s);
                conexoes.execute(() -> atender(s));
            } catch (SocketException e) {
                // porta fechada por parar()
            } catch (IOException e) {
                LOG.error("Erro ao aceitar conexão", e);
            }
        }
    }

    private void atender(Socket s) {
        String ip = s.getInetAddress().getHostAddress();
        String endereco = ip + ":" + s.getPort();
        LOG.info("[{}] Conexão aberta {}", lado, endereco);
        try (InputStream in = new BufferedInputStream(s.getInputStream());
                OutputStream out = new BufferedOutputStream(s.getOutputStream())) {
            while (true) {
                Mensagem req;
                try {
                    req = CodecMensagem.ler(in);
                } catch (MensagemMalformadaException e) {
                    LOG.warn("[{}] Mensagem malformada de {}: {}", lado, endereco, e.getMessage());
                    responder(out, endereco, malformada(), null);
                    continue;
                }
                if (req == null) {
                    break;
                }
                long inicio = System.nanoTime();
                MENSAGENS.info(FormatadorLog.formatarTroca(lado, Direcao.RECEBIDA, endereco, req, null));
                Mensagem resp = processador.processar(req, ip);
                responder(out, endereco, resp, (System.nanoTime() - inicio) / 1_000_000);
            }
        } catch (MensagemMuitoGrandeException e) {
            LOG.warn("[{}] {} de {}; conexão encerrada", lado, e.getMessage(), endereco);
        } catch (IOException e) {
            LOG.debug("[{}] Conexão {} interrompida: {}", lado, endereco, e.getMessage());
        } finally {
            abertas.remove(s);
            fechar(s);
            LOG.info("[{}] Conexão encerrada {}", lado, endereco);
        }
    }

    private void responder(OutputStream out, String endereco, Mensagem resp, Long duracaoMs) throws IOException {
        out.write(CodecMensagem.codificar(resp));
        out.flush();
        MENSAGENS.info(FormatadorLog.formatarTroca(lado, Direcao.ENVIADA, endereco, resp, duracaoMs));
    }

    private static Mensagem malformada() {
        return new Mensagem()
                .adicionar(Rotulo.CODIGO_RETORNO, CodigoRetorno.MENSAGEM_MALFORMADA.codigo())
                .adicionar(Rotulo.MENSAGEM_RETORNO, CodigoRetorno.MENSAGEM_MALFORMADA.descricao());
    }

    private static void fechar(Socket s) {
        try {
            s.close();
        } catch (IOException e) {
            LOG.debug("Erro ao fechar conexão", e);
        }
    }
}
