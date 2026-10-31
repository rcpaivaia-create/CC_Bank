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
     * @return a mensagem, ou {@code null} se o stream terminou antes do primeiro byte
     */
    public static Mensagem ler(InputStream in) throws IOException {
        Mensagem mensagem = new Mensagem();
        ByteArrayOutputStream linha = new ByteArrayOutputStream();
        int total = 0;
        while (true) {
            int b = in.read();
            if (b < 0) {
                if (total == 0) {
                    return null;
                }
                throw new MensagemMalformadaException("fim do stream no meio da mensagem");
            }
            total++;
            if (total > TAMANHO_MAXIMO) {
                throw new MensagemMuitoGrandeException("mensagem excede " + TAMANHO_MAXIMO + " bytes");
            }
            if (b != '\n') {
                linha.write(b);
                continue;
            }
            byte[] bytes = linha.toByteArray();
            linha.reset();
            int tamanho = bytes.length;
            if (tamanho > 0 && bytes[tamanho - 1] == '\r') {
                tamanho--;
            }
            if (tamanho == 0) {
                if (mensagem.tamanho() == 0) {
                    throw new MensagemMalformadaException("mensagem sem campos");
                }
                return mensagem;
            }
            adicionarLinha(mensagem, decodificarUtf8(bytes, tamanho));
        }
    }

    public static Mensagem decodificar(byte[] bytes) throws IOException {
        Mensagem m = ler(new ByteArrayInputStream(bytes));
        if (m == null) {
            throw new MensagemMalformadaException("mensagem vazia");
        }
        return m;
    }

    private static String decodificarUtf8(byte[] bytes, int tamanho) throws MensagemMalformadaException {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return decoder.decode(ByteBuffer.wrap(bytes, 0, tamanho)).toString();
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
