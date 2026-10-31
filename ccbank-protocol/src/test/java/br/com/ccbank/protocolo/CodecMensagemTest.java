package br.com.ccbank.protocolo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class CodecMensagemTest {

    private static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static String repetir(String s, int vezes) {
        StringBuilder sb = new StringBuilder(s.length() * vezes);
        for (int i = 0; i < vezes; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    @Test
    void codificaNoFormatoDaSpec() {
        Mensagem m = new Mensagem().adicionar("00001", "login").adicionar("00002", "rodrigop").adicionar("00003", "senha123");
        assertEquals("00001=login\n00002=rodrigop\n00003=senha123\n\n",
                new String(CodecMensagem.codificar(m), StandardCharsets.UTF_8));
    }

    @Test
    void idaEVoltaComAcentoIgualERepeticao() throws IOException {
        Mensagem m = new Mensagem().adicionar("00093", "Maria Conceição").adicionar("54321", "a=b=c")
                .adicionar("00081", "x").adicionar("00081", "y").adicionar("00030", "");
        assertEquals(m, CodecMensagem.decodificar(CodecMensagem.codificar(m)));
    }

    @Test
    void leDuasMensagensSeguidasDoMesmoStream() throws IOException {
        InputStream in = new ByteArrayInputStream(utf8("00001=saldo\n\n00001=saque\n\n"));
        assertEquals(Optional.of("saldo"), CodecMensagem.ler(in).primeiro("00001"));
        assertEquals(Optional.of("saque"), CodecMensagem.ler(in).primeiro("00001"));
        assertNull(CodecMensagem.ler(in));
    }

    @Test
    void aceitaCrlf() throws IOException {
        Mensagem m = CodecMensagem.decodificar(utf8("00001=saldo\r\n00002=rodrigop\r\n\r\n"));
        assertEquals(Optional.of("saldo"), m.primeiro("00001"));
        assertEquals(Optional.of("rodrigop"), m.primeiro("00002"));
    }

    @Test
    void rejeitaLinhasInvalidas() {
        assertThrows(MensagemMalformadaException.class, () -> CodecMensagem.decodificar(utf8("00001saldo\n\n")));
        assertThrows(MensagemMalformadaException.class, () -> CodecMensagem.decodificar(utf8("0001=saldo\n\n")));
        assertThrows(MensagemMalformadaException.class, () -> CodecMensagem.decodificar(utf8("abcde=saldo\n\n")));
        assertThrows(MensagemMalformadaException.class, () -> CodecMensagem.decodificar(utf8("\n")));
    }

    @Test
    void rejeitaFimNoMeioDaMensagem() {
        assertThrows(MensagemMalformadaException.class, () -> CodecMensagem.decodificar(utf8("00001=saldo\n")));
        assertThrows(MensagemMalformadaException.class, () -> CodecMensagem.decodificar(utf8("00001=sal")));
    }

    @Test
    void rejeitaUtf8Invalido() {
        byte[] b = {'0', '0', '0', '0', '1', '=', (byte) 0xC3, (byte) 0x28, '\n', '\n'};
        assertThrows(MensagemMalformadaException.class, () -> CodecMensagem.decodificar(b));
    }

    @Test
    void limiteDeTamanhoContaBytes() throws IOException {
        // "00093=" (6) + valor + "\n" (1) + "\n" (1) = 65536 -> valor com 65528 bytes
        String cabe = repetir("ç", 65528 / 2);
        assertNotNull(CodecMensagem.decodificar(utf8("00093=" + cabe + "\n\n")));
        String passa = repetir("a", 65529);
        assertThrows(MensagemMuitoGrandeException.class, () -> CodecMensagem.decodificar(utf8("00093=" + passa + "\n\n")));
    }

    @Test
    void naoLeAlemDoLimite() {
        InputStream infinito = new InputStream() {
            @Override
            public int read() {
                return 'a';
            }
        };
        assertThrows(MensagemMuitoGrandeException.class, () -> CodecMensagem.ler(infinito));
    }

    @Test
    void mensagemMalformadaNaoDessincronizaOStream() throws IOException {
        InputStream in = new ByteArrayInputStream(utf8(
                "00001=saque\nlixo\n00002=rodrigop\n00030=150.00\n\n00001=saldo\n\n"));
        assertThrows(MensagemMalformadaException.class, () -> CodecMensagem.ler(in));
        assertEquals(new Mensagem().adicionar("00001", "saldo"), CodecMensagem.ler(in));
        assertNull(CodecMensagem.ler(in));
    }

    @Test
    void lerQuadroDevolveBytesCrusAteALinhaVazia() throws IOException {
        InputStream in = new ByteArrayInputStream(utf8("00001=a\r\n\r\nlixo sem igual\n\n"));
        assertArrayEquals(utf8("00001=a\r\n\r\n"), CodecMensagem.lerQuadro(in));
        assertArrayEquals(utf8("lixo sem igual\n\n"), CodecMensagem.lerQuadro(in));
        assertNull(CodecMensagem.lerQuadro(in));
    }

    @Test
    void limiteRejeitaMultibyteAcimaDe65536Bytes() {
        // "00093=" (6) + 32765 x "ç" (65530) + "\n\n" (2) = 65538 bytes, mas só 32773 caracteres
        String valor = repetir("ç", 32765);
        assertThrows(MensagemMuitoGrandeException.class, () -> CodecMensagem.decodificar(utf8("00093=" + valor + "\n\n")));
    }

    @Test
    void codificarMensagemVaziaFalha() {
        assertThrows(IllegalArgumentException.class, () -> CodecMensagem.codificar(new Mensagem()));
    }
}
