package br.com.ccbank.interceptador.proxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import br.com.ccbank.protocolo.CodecMensagem;

/**
 * Reenvia uma requisição capturada diretamente ao servidor de destino, {@code vezes} vezes,
 * numa conexão nova — sem passar pelo mediador. É o ataque de replay do v1 (seção 7.3, #8).
 */
public final class Replay {

    private static final int TIMEOUT_CONEXAO_MS = 5_000;

    private Replay() {
    }

    /**
     * @param vezes número de reenvios; {@code <= 0} não conecta e devolve lista vazia
     * @return as respostas recebidas, uma por reenvio
     * @throws IOException se o servidor encerrar antes de responder a algum reenvio
     */
    public static List<byte[]> reenviar(byte[] requisicao, String host, int porta, int vezes) throws IOException {
        List<byte[]> respostas = new ArrayList<>();
        if (vezes <= 0) {
            return respostas;
        }
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, porta), TIMEOUT_CONEXAO_MS);
            try (InputStream in = new BufferedInputStream(s.getInputStream());
                    OutputStream out = new BufferedOutputStream(s.getOutputStream())) {
                for (int i = 0; i < vezes; i++) {
                    out.write(requisicao);
                    out.flush();
                    byte[] resposta = CodecMensagem.lerQuadro(in);
                    if (resposta == null) {
                        throw new IOException("servidor encerrou antes da resposta " + (i + 1));
                    }
                    respostas.add(resposta);
                }
            }
        }
        return respostas;
    }
}
