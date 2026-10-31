package br.com.ccbank.protocolo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * Codec texto do protocolo: uma linha {@code RRRRR=valor} por campo, terminada
 * por {@code \n}, e uma linha vazia encerrando a mensagem. UTF-8.
 */
public final class CodecMensagem {

    public static final int TAMANHO_MAXIMO = 65536;

    private CodecMensagem() {
    }

    public static byte[] codificar(Mensagem m) {
        if (m.tamanho() == 0) {
            throw new IllegalArgumentException("mensagem sem campos");
        }
        StringBuilder sb = new StringBuilder();
        for (Campo c : m.campos()) {
            sb.append(c.rotulo()).append('=').append(c.valor()).append('\n');
        }
        sb.append('\n');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Lê uma mensagem do stream. O chamador deve passar um stream bufferizado,
     * pois a leitura é feita byte a byte.
     *
     * <p>{@link MensagemMalformadaException} por conteúdo inválido consome o quadro
     * inteiro, e o stream continua utilizável. Fim do stream no meio da mensagem e
     * {@link MensagemMuitoGrandeException} deixam o stream inutilizável: feche a conexão.
     *
     * @return a mensagem, ou {@code null} se o stream terminou antes do primeiro byte
     */
    public static Mensagem ler(InputStream in) throws IOException {
        byte[] quadro = lerQuadro(in);
        return quadro == null ? null : analisar(quadro);
    }

    /**
     * Lê os bytes crus de um quadro, até e incluindo a linha vazia, sem interpretar
     * os campos. Aplica o limite de {@link #TAMANHO_MAXIMO} bytes.
     *
     * @return o quadro, ou {@code null} se o stream terminou antes do primeiro byte
     */
    public static byte[] lerQuadro(InputStream in) throws IOException {
        ByteArrayOutputStream quadro = new ByteArrayOutputStream();
        int bytesNaLinha = 0;
        boolean linhaSoCr = false;
        while (true) {
            int b = in.read();
            if (b < 0) {
                if (quadro.size() == 0) {
                    return null;
                }
                throw new MensagemMalformadaException("fim do stream no meio da mensagem");
            }
            if (quadro.size() >= TAMANHO_MAXIMO) {
                throw new MensagemMuitoGrandeException("mensagem excede " + TAMANHO_MAXIMO + " bytes");
            }
            quadro.write(b);
            if (b == '\n') {
                if (bytesNaLinha == 0 || linhaSoCr) {
                    return quadro.toByteArray();
                }
                bytesNaLinha = 0;
                linhaSoCr = false;
            } else {
                bytesNaLinha++;
                linhaSoCr = bytesNaLinha == 1 && b == '\r';
            }
        }
    }

    public static Mensagem decodificar(byte[] bytes) throws IOException {
        byte[] quadro = lerQuadro(new ByteArrayInputStream(bytes));
        if (quadro == null) {
            throw new MensagemMalformadaException("mensagem vazia");
        }
        return analisar(quadro);
    }

    /** Interpreta um quadro completo, como devolvido por {@link #lerQuadro}. */
    private static Mensagem analisar(byte[] quadro) throws MensagemMalformadaException {
        Mensagem mensagem = new Mensagem();
        int inicio = 0;
        for (int i = 0; i < quadro.length; i++) {
            if (quadro[i] != '\n') {
                continue;
            }
            int tamanho = i - inicio;
            if (tamanho > 0 && quadro[i - 1] == '\r') {
                tamanho--;
            }
            if (tamanho == 0) {
                break;
            }
            adicionarLinha(mensagem, decodificarUtf8(quadro, inicio, tamanho));
            inicio = i + 1;
        }
        if (mensagem.tamanho() == 0) {
            throw new MensagemMalformadaException("mensagem sem campos");
        }
        return mensagem;
    }

    private static String decodificarUtf8(byte[] bytes, int inicio, int tamanho) throws MensagemMalformadaException {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return decoder.decode(ByteBuffer.wrap(bytes, inicio, tamanho)).toString();
        } catch (CharacterCodingException e) {
            throw new MensagemMalformadaException("linha não é UTF-8 válido");
        }
    }

    private static void adicionarLinha(Mensagem mensagem, String linha) throws MensagemMalformadaException {
        int igual = linha.indexOf('=');
        if (igual < 0) {
            throw new MensagemMalformadaException("linha sem '=': " + linha);
        }
        try {
            mensagem.adicionar(linha.substring(0, igual), linha.substring(igual + 1));
        } catch (IllegalArgumentException e) {
            throw new MensagemMalformadaException(e.getMessage());
        }
    }
}
