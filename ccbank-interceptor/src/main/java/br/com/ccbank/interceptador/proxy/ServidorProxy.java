package br.com.ccbank.interceptador.proxy;

import java.io.IOException;
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

import br.com.ccbank.interceptador.config.LadoInterceptador;
import br.com.ccbank.interceptador.mediacao.Mediador;
import br.com.ccbank.interceptador.mediacao.MediadorRegras;
import br.com.ccbank.interceptador.mediacao.OuvinteTroca;
import br.com.ccbank.interceptador.regras.MotorRegras;

/**
 * Proxy TCP de um lado do interceptador: escuta uma porta, e para cada conexão aceita abre uma
 * conexão ao servidor de destino e trata o par numa thread do pool. Mesmo molde do servidor.
 */
public final class ServidorProxy {

    private static final Logger LOG = LoggerFactory.getLogger(ServidorProxy.class);

    private final int portaEscuta;
    private final String hostDestino;
    private final int portaDestino;
    private final Mediador mediador;
    private final OuvinteTroca ouvinte;
    private final String sufixoLog;
    private final Set<Socket> abertas = ConcurrentHashMap.newKeySet();
    private final AtomicInteger contador = new AtomicInteger();
    private final ExecutorService pares;
    private volatile ServerSocket serverSocket;

    public ServidorProxy(int portaEscuta, String hostDestino, int portaDestino, Mediador mediador,
            OuvinteTroca ouvinte, String sufixoLog) {
        this.portaEscuta = portaEscuta;
        this.hostDestino = hostDestino;
        this.portaDestino = portaDestino;
        this.mediador = mediador;
        this.ouvinte = ouvinte;
        this.sufixoLog = sufixoLog;
        this.pares = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "par-" + sufixoLog + "-" + contador.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    /** Conveniência que preserva o headless da 2a: motor de regras e sem observação. */
    public ServidorProxy(int portaEscuta, String hostDestino, int portaDestino, MotorRegras motor, String sufixoLog) {
        this(portaEscuta, hostDestino, portaDestino, new MediadorRegras(motor), OuvinteTroca.NULO, sufixoLog);
    }

    public static ServidorProxy doLado(LadoInterceptador lado, Mediador mediador, OuvinteTroca ouvinte) {
        return new ServidorProxy(lado.portaEscuta(), lado.hostDestino(), lado.portaDestino(), mediador, ouvinte,
                lado.sufixoLog());
    }

    public static ServidorProxy doLado(LadoInterceptador lado, MotorRegras motor) {
        return new ServidorProxy(lado.portaEscuta(), lado.hostDestino(), lado.portaDestino(), motor, lado.sufixoLog());
    }

    /** Abre a porta e passa a aceitar conexões numa thread própria; não bloqueia. */
    public void iniciar() throws IOException {
        serverSocket = new ServerSocket(portaEscuta);
        Thread aceitador = new Thread(this::aceitar, "aceitador-proxy-" + sufixoLog);
        aceitador.start();
    }

    public int portaLocal() {
        return serverSocket.getLocalPort();
    }

    /** Número de conexões de cliente ainda abertas (visível para teste). */
    int conexoesAtivas() {
        return abertas.size();
    }

    public void parar() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            LOG.debug("[{}] erro ao fechar a porta", sufixoLog, e);
        }
        for (Socket s : abertas) {
            try {
                s.close();
            } catch (IOException e) {
                LOG.debug("[{}] erro ao fechar conexão", sufixoLog, e);
            }
        }
        pares.shutdownNow();
    }

    private void aceitar() {
        while (!serverSocket.isClosed()) {
            try {
                Socket cliente = serverSocket.accept();
                abertas.add(cliente);
                pares.execute(() -> new ParDeConexoes(cliente, hostDestino, portaDestino, mediador, ouvinte,
                        sufixoLog, abertas).executar());
            } catch (SocketException e) {
                // porta fechada por parar()
            } catch (IOException e) {
                LOG.error("[{}] erro ao aceitar conexão", sufixoLog, e);
            }
        }
    }
}
