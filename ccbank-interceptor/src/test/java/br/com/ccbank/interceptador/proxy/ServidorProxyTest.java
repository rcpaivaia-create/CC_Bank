package br.com.ccbank.interceptador.proxy;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.jupiter.api.Test;

import br.com.ccbank.interceptador.regras.Acao;
import br.com.ccbank.interceptador.regras.Condicao;
import br.com.ccbank.interceptador.regras.MotorRegras;
import br.com.ccbank.interceptador.regras.Regra;
import br.com.ccbank.interceptador.regras.Sentido;
import br.com.ccbank.protocolo.CodecMensagem;
import br.com.ccbank.protocolo.Mensagem;

class ServidorProxyTest {

    private static byte[] quadro(Mensagem m) {
        return CodecMensagem.codificar(m);
    }

    /** Servidor-eco de teste: aceita uma conexão, lê um quadro (guarda os bytes) e devolve uma resposta. */
    private static final class ServidorFalso implements AutoCloseable {
        private final ServerSocket ss;
        private final byte[] resposta;
        private volatile byte[] recebido;

        ServidorFalso(byte[] resposta) throws IOException {
            this.ss = new ServerSocket(0);
            this.resposta = resposta;
            Thread t = new Thread(this::rodar, "servidor-falso");
            t.setDaemon(true);
            t.start();
        }

        int porta() {
            return ss.getLocalPort();
        }

        private void rodar() {
            try (Socket s = ss.accept();
                    InputStream in = new BufferedInputStream(s.getInputStream());
                    OutputStream out = new BufferedOutputStream(s.getOutputStream())) {
                recebido = CodecMensagem.lerQuadro(in);
                if (recebido != null && resposta != null) {
                    out.write(resposta);
                    out.flush();
                }
            } catch (IOException e) {
                // conexão derrubada pelo proxy ou fim do teste
            }
        }

        @Override
        public void close() throws IOException {
            ss.close();
        }
    }

    private ServidorProxy proxy;

    private Socket abrirProxy(ServidorFalso eco, MotorRegras motor) throws IOException {
        proxy = new ServidorProxy(0, "127.0.0.1", eco.porta(), motor, "teste");
        proxy.iniciar();
        Socket cliente = new Socket("127.0.0.1", proxy.portaLocal());
        cliente.setSoTimeout(5000);
        return cliente;
    }

    private static MotorRegras semRegras() {
        return new MotorRegras(emptyList());
    }

    @org.junit.jupiter.api.AfterEach
    void parar() {
        if (proxy != null) {
            proxy.parar();
        }
    }

    @Test
    void passagemEntregaBytesIdenticos() throws Exception {
        byte[] req = quadro(new Mensagem().adicionar("00001", "login").adicionar("00002", "rodrigop"));
        byte[] resp = quadro(new Mensagem().adicionar("00090", "000"));
        try (ServidorFalso eco = new ServidorFalso(resp)) {
            try (Socket cliente = abrirProxy(eco, semRegras());
                    OutputStream out = cliente.getOutputStream();
                    InputStream in = cliente.getInputStream()) {
                out.write(req);
                out.flush();
                byte[] recebidoPeloCliente = CodecMensagem.lerQuadro(in);
                assertArrayEquals(req, eco.recebido);
                assertArrayEquals(resp, recebidoPeloCliente);
            }
        }
    }

    @Test
    void passagemPreservaCrlf() throws Exception {
        byte[] reqCrlf = "00001=saque\r\n00002=rodrigop\r\n\r\n".getBytes(UTF_8);
        byte[] resp = quadro(new Mensagem().adicionar("00090", "000"));
        try (ServidorFalso eco = new ServidorFalso(resp)) {
            try (Socket cliente = abrirProxy(eco, semRegras());
                    OutputStream out = cliente.getOutputStream();
                    InputStream in = cliente.getInputStream()) {
                out.write(reqCrlf);
                out.flush();
                CodecMensagem.lerQuadro(in);
                assertArrayEquals(reqCrlf, eco.recebido);
            }
        }
    }

    @Test
    void regraSetNaRequisicaoChegaAdulteradaAoServidor() throws Exception {
        MotorRegras motor = new MotorRegras(singletonList(new Regra("saque-centavos", true, Sentido.REQUISICAO,
                new Condicao(singletonMap("00001", "saque")), singletonList(new Acao(Acao.Tipo.SET, "00030", "0.01")))));
        byte[] req = quadro(new Mensagem().adicionar("00001", "saque").adicionar("00030", "500.00"));
        byte[] resp = quadro(new Mensagem().adicionar("00090", "000"));
        try (ServidorFalso eco = new ServidorFalso(resp)) {
            try (Socket cliente = abrirProxy(eco, motor);
                    OutputStream out = cliente.getOutputStream();
                    InputStream in = cliente.getInputStream()) {
                out.write(req);
                out.flush();
                CodecMensagem.lerQuadro(in);
                assertEquals(java.util.Optional.of("0.01"), CodecMensagem.decodificar(eco.recebido).primeiro("00030"));
            }
        }
    }

    @Test
    void regraNaRespostaChegaAdulteradaAoCliente() throws Exception {
        MotorRegras motor = new MotorRegras(singletonList(new Regra("resposta-falsa", true, Sentido.RESPOSTA,
                new Condicao(singletonMap("00090", "003")), singletonList(new Acao(Acao.Tipo.SET, "00090", "000")))));
        byte[] req = quadro(new Mensagem().adicionar("00001", "saldo"));
        byte[] resp = quadro(new Mensagem().adicionar("00090", "003"));
        try (ServidorFalso eco = new ServidorFalso(resp)) {
            try (Socket cliente = abrirProxy(eco, motor);
                    OutputStream out = cliente.getOutputStream();
                    InputStream in = cliente.getInputStream()) {
                out.write(req);
                out.flush();
                byte[] recebidoPeloCliente = CodecMensagem.lerQuadro(in);
                assertEquals(java.util.Optional.of("000"), CodecMensagem.decodificar(recebidoPeloCliente).primeiro("00090"));
            }
        }
    }

    @Test
    void quadroNaoDecodificavelPassaEmModoOpaco() throws Exception {
        byte[] opaco = "sem-igual\n\n".getBytes(UTF_8);
        byte[] resp = quadro(new Mensagem().adicionar("00090", "000"));
        try (ServidorFalso eco = new ServidorFalso(resp)) {
            try (Socket cliente = abrirProxy(eco, semRegras());
                    OutputStream out = cliente.getOutputStream();
                    InputStream in = cliente.getInputStream()) {
                out.write(opaco);
                out.flush();
                CodecMensagem.lerQuadro(in);
                assertArrayEquals(opaco, eco.recebido);
            }
        }
    }

    @Test
    void conexaoEncerradaSaiDoConjunto() throws Exception {
        byte[] req = quadro(new Mensagem().adicionar("00001", "login"));
        byte[] resp = quadro(new Mensagem().adicionar("00090", "000"));
        try (ServidorFalso eco = new ServidorFalso(resp)) {
            Socket cliente = abrirProxy(eco, semRegras());
            cliente.getOutputStream().write(req);
            cliente.getOutputStream().flush();
            CodecMensagem.lerQuadro(cliente.getInputStream());
            cliente.close();
            long fim = System.currentTimeMillis() + 3000;
            while (proxy.conexoesAtivas() > 0 && System.currentTimeMillis() < fim) {
                Thread.sleep(20);
            }
            assertEquals(0, proxy.conexoesAtivas());
        }
    }

    @Test
    void quadroAcimaDe64kDerrubaOPar() throws Exception {
        byte[] gigante = new byte[70000];
        java.util.Arrays.fill(gigante, (byte) 'x');
        try (ServidorFalso eco = new ServidorFalso(null)) {
            try (Socket cliente = abrirProxy(eco, semRegras());
                    OutputStream out = cliente.getOutputStream();
                    InputStream in = cliente.getInputStream()) {
                try {
                    out.write(gigante);
                    out.flush();
                } catch (IOException e) {
                    // o proxy pode derrubar antes de recebermos tudo
                }
                int r;
                try {
                    r = in.read();
                } catch (IOException e) {
                    r = -1;
                }
                assertEquals(-1, r);
            }
        }
    }
}
