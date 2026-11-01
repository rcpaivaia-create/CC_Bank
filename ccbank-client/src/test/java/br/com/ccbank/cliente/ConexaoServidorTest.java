package br.com.ccbank.cliente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;

class ConexaoServidorTest {

    @Test
    void fecharNaoEsperaRespostaRetida() throws Exception {
        try (ServerSocket servidor = new ServerSocket(0)) {
            // Servidor que aceita e nunca responde (como o interceptador em modo manual)
            CompletableFuture<Socket> aceito = CompletableFuture.supplyAsync(() -> {
                try {
                    return servidor.accept();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
            ConexaoServidor conexao = new ConexaoServidor(LadoCliente.ESQUERDO, "127.0.0.1", servidor.getLocalPort());
            CompletableFuture<Throwable> envio = CompletableFuture.supplyAsync(() -> {
                try {
                    conexao.enviar(new Mensagem().adicionar(Rotulo.TRANSACAO, "login"));
                    return null;
                } catch (IOException e) {
                    return e;
                }
            });
            Socket lado = aceito.get(5, TimeUnit.SECONDS);
            Thread.sleep(300); // o envio já está bloqueado na leitura

            long inicio = System.nanoTime();
            conexao.fechar();
            long ms = (System.nanoTime() - inicio) / 1_000_000;

            assertTrue(ms < 1_000, "fechar() demorou " + ms + " ms");
            assertTrue(envio.get(5, TimeUnit.SECONDS) instanceof IOException);
            lado.close();
        }
    }

    @Test
    void portaDeInterceptadorNomeiaInterceptador() {
        assertEquals("Interceptador", ConexaoServidor.nomeDoServico(LadoCliente.ESQUERDO, 8001));
        assertEquals("Interceptador", ConexaoServidor.nomeDoServico(LadoCliente.DIREITO, 8002));
    }

    @Test
    void portaDeServidorNomeiaServidor() {
        assertEquals("Servidor", ConexaoServidor.nomeDoServico(LadoCliente.ESQUERDO, 9001));
        assertEquals("Servidor", ConexaoServidor.nomeDoServico(LadoCliente.DIREITO, 9002));
    }
}
