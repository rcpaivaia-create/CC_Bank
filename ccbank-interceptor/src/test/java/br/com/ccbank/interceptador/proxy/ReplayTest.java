package br.com.ccbank.interceptador.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import br.com.ccbank.protocolo.CodecMensagem;
import br.com.ccbank.protocolo.Mensagem;

class ReplayTest {

    /** Servidor-eco em laço: lê quadros e responde, até {@code maxQuadros} (ou sem limite se < 0). */
    private static final class EcoEmLaco implements AutoCloseable {
        private final ServerSocket ss;
        private final byte[] resposta;
        private final int maxQuadros;
        private final AtomicInteger recebidos = new AtomicInteger();

        EcoEmLaco(byte[] resposta, int maxQuadros) throws IOException {
            this.ss = new ServerSocket(0);
            this.resposta = resposta;
            this.maxQuadros = maxQuadros;
            Thread t = new Thread(this::rodar, "eco-em-laco");
            t.setDaemon(true);
            t.start();
        }

        int porta() {
            return ss.getLocalPort();
        }

        int quadrosRecebidos() {
            return recebidos.get();
        }

        private void rodar() {
            try (Socket s = ss.accept();
                    InputStream in = new BufferedInputStream(s.getInputStream());
                    OutputStream out = new BufferedOutputStream(s.getOutputStream())) {
                while (maxQuadros < 0 || recebidos.get() < maxQuadros) {
                    byte[] q = CodecMensagem.lerQuadro(in);
                    if (q == null) {
                        break;
                    }
                    recebidos.incrementAndGet();
                    out.write(resposta);
                    out.flush();
                }
            } catch (IOException e) {
                // fim do teste
            }
        }

        @Override
        public void close() throws IOException {
            ss.close();
        }
    }

    @Test
    void reenviaNVezesERecebeNRespostas() throws Exception {
        byte[] req = CodecMensagem.codificar(new Mensagem().adicionar("00001", "saque"));
        byte[] resp = CodecMensagem.codificar(new Mensagem().adicionar("00090", "000"));
        try (EcoEmLaco eco = new EcoEmLaco(resp, -1)) {
            List<byte[]> respostas = Replay.reenviar(req, "127.0.0.1", eco.porta(), 3);
            assertEquals(3, respostas.size());
            assertEquals(3, eco.quadrosRecebidos());
        }
    }

    @Test
    void vezesZeroNaoConecta() throws Exception {
        assertTrue(Replay.reenviar(new byte[] {1}, "127.0.0.1", 1, 0).isEmpty());
    }

    @Test
    void servidorQueFechaCedoLancaIOException() throws Exception {
        byte[] req = CodecMensagem.codificar(new Mensagem().adicionar("00001", "saque"));
        byte[] resp = CodecMensagem.codificar(new Mensagem().adicionar("00090", "000"));
        try (EcoEmLaco eco = new EcoEmLaco(resp, 1)) {
            assertThrows(IOException.class, () -> Replay.reenviar(req, "127.0.0.1", eco.porta(), 3));
        }
    }
}
