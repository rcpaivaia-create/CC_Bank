package br.com.ccbank.cliente;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.ccbank.protocolo.CodecMensagem;
import br.com.ccbank.protocolo.Direcao;
import br.com.ccbank.protocolo.FormatadorLog;
import br.com.ccbank.protocolo.Mensagem;

/**
 * Conexão TCP persistente de uma metade do cliente. Conecta sob demanda; qualquer erro
 * fecha o socket e a próxima chamada reconecta. Uma requisição por vez.
 */
public final class ConexaoServidor {

    static final int TIMEOUT_CONEXAO_MS = 5_000;
    static final int TIMEOUT_LEITURA_MS = 120_000;

    private final LadoCliente lado;
    private final String host;
    private final int porta;
    private final Logger log;
    /** Volátil: {@link #fechar()} fecha sem o lock de {@link #enviar}, acordando uma leitura bloqueada. */
    private volatile Socket socket;
    private InputStream in;
    private OutputStream out;

    public ConexaoServidor(LadoCliente lado, String host, int porta) {
        this.lado = lado;
        this.host = host;
        this.porta = porta;
        this.log = LoggerFactory.getLogger("ccbank.cliente." + lado.sufixoLog());
    }

    public String endereco() {
        return host + ":" + porta;
    }

    /** Nome do serviço de destino conforme a porta: a porta do interceptador do lado, ou o servidor. */
    static String nomeDoServico(LadoCliente lado, int porta) {
        return porta == lado.portaInterceptador() ? "Interceptador" : "Servidor";
    }

    public synchronized Resposta enviar(Mensagem req) throws IOException {
        conectarSeNecessario();
        long inicio = System.nanoTime();
        try {
            out.write(CodecMensagem.codificar(req));
            out.flush();
            log.info(FormatadorLog.formatarTroca(lado.sigla(), Direcao.ENVIADA, endereco(), req, null));
            Mensagem resp = CodecMensagem.ler(in);
            if (resp == null) {
                throw new IOException("Conexão encerrada pelo servidor");
            }
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            log.info(FormatadorLog.formatarTroca(lado.sigla(), Direcao.RECEBIDA, endereco(), resp, ms));
            return new Resposta(req, resp, ms);
        } catch (IOException e) {
            log.warn("[{}] Falha na comunicação com {}: {}", lado.sigla(), endereco(), e.getMessage());
            fechar();
            throw e;
        }
    }

    /**
     * Fecha a conexão sem esperar uma requisição em andamento: a leitura bloqueada recebe
     * uma exceção e {@link #enviar} a repassa. A próxima chamada reconecta.
     */
    public void fechar() {
        Socket s = socket;
        socket = null;
        if (s != null) {
            try {
                s.close();
            } catch (IOException e) {
                log.debug("Erro ao fechar conexão", e);
            }
        }
    }

    private void conectarSeNecessario() throws ServicoIndisponivelException {
        if (socket != null) {
            return;
        }
        Socket s = new Socket();
        try {
            s.connect(new InetSocketAddress(host, porta), TIMEOUT_CONEXAO_MS);
            s.setSoTimeout(TIMEOUT_LEITURA_MS);
            in = new BufferedInputStream(s.getInputStream());
            out = new BufferedOutputStream(s.getOutputStream());
            socket = s;
        } catch (IOException e) {
            try {
                s.close();
            } catch (IOException ignorada) {
                // nada a fazer
            }
            String mensagem = nomeDoServico(lado, porta) + " não disponível em " + endereco();
            log.warn("[{}] {}", lado.sigla(), mensagem);
            throw new ServicoIndisponivelException(mensagem, e);
        }
    }
}
