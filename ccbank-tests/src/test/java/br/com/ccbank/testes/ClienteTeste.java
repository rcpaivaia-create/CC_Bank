package br.com.ccbank.testes;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import br.com.ccbank.protocolo.CodecMensagem;
import br.com.ccbank.protocolo.Mensagem;

/** Cliente TCP mínimo para os testes: uma conexão, requisição/resposta síncrona. */
final class ClienteTeste implements Closeable {

    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;

    ClienteTeste(int porta) throws IOException {
        socket = new Socket("127.0.0.1", porta);
        socket.setSoTimeout(15_000);
        in = new BufferedInputStream(socket.getInputStream());
        out = socket.getOutputStream();
    }

    Mensagem enviar(Mensagem m) throws IOException {
        enviarBytes(CodecMensagem.codificar(m));
        return receber();
    }

    void enviarBytes(byte[] bytes) throws IOException {
        out.write(bytes);
        out.flush();
    }

    Mensagem receber() throws IOException {
        return CodecMensagem.ler(in);
    }

    /** {@code true} se o servidor fechou a conexão (fim do stream ou reset). */
    boolean conexaoFechada() throws IOException {
        try {
            return in.read() < 0;
        } catch (SocketException e) {
            return true;
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
