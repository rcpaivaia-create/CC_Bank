package br.com.ccbank.cliente.tela;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.ccbank.cliente.LadoCliente;
import br.com.ccbank.cliente.Resposta;
import br.com.ccbank.cliente.UltimoLogin;
import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;

class PainelLadoTest {

    @Test
    void estadoInicial() {
        PainelLado p = new PainelLado(LadoCliente.ESQUERDO);
        assertFalse(p.logado());
        assertEquals("login", p.cartaoAtual());
        assertTrue(p.checkInterceptador().isEnabled());
        assertFalse(p.checkInterceptador().isSelected());
        assertEquals("127.0.0.1:9001", p.cliente().endereco());
    }

    @Test
    void preencheUsuarioComUltimoLogin(@TempDir Path dir) throws Exception {
        UltimoLogin ul = new UltimoLogin(dir.resolve("esq.dat"));
        ul.salvar("ana.lima");
        assertEquals("ana.lima", new PainelLado(LadoCliente.ESQUERDO, ul).usuarioLogin());
    }

    @Test
    void semArquivoCampoVazio(@TempDir Path dir) {
        assertEquals("", new PainelLado(LadoCliente.ESQUERDO, new UltimoLogin(dir.resolve("nada.dat"))).usuarioLogin());
    }

    @Test
    void loginEfetuadoGrava(@TempDir Path dir) {
        UltimoLogin ul = new UltimoLogin(dir.resolve("esq.dat"));
        new PainelLado(LadoCliente.ESQUERDO, ul).loginEfetuado("carla.dias");
        assertEquals(Optional.of("carla.dias"), ul.ler());
    }

    private static Resposta respostaLogin(String codigo) {
        return new Resposta(new Mensagem().adicionar(Rotulo.TRANSACAO, "login"),
                new Mensagem().adicionar(Rotulo.CODIGO_RETORNO, codigo).adicionar(Rotulo.MENSAGEM_RETORNO, "x"), 1);
    }

    @Test
    void loginAceitoEhGravadoComoFoiEnviado(@TempDir Path dir) {
        UltimoLogin ul = new UltimoLogin(dir.resolve("esq.dat"));
        new PainelLado(LadoCliente.ESQUERDO, ul).concluirLogin("rodrigop", respostaLogin("000"));
        assertEquals(Optional.of("rodrigop"), ul.ler());
    }

    @Test
    void loginRecusadoNaoEhGravado(@TempDir Path dir) throws Exception {
        UltimoLogin ul = new UltimoLogin(dir.resolve("esq.dat"));
        ul.salvar("ana.lima");
        PainelLado p = new PainelLado(LadoCliente.ESQUERDO, ul);
        p.concluirLogin("intruso", respostaLogin("001"));
        assertEquals(Optional.of("ana.lima"), ul.ler());
        assertEquals("login", p.cartaoAtual());
    }

    @Test
    void ladosNaoSeMisturam(@TempDir Path dir) throws Exception {
        UltimoLogin esq = new UltimoLogin(dir.resolve("esq.dat"));
        UltimoLogin dir2 = new UltimoLogin(dir.resolve("dir.dat"));
        dir2.salvar("hugo.pires");
        new PainelLado(LadoCliente.ESQUERDO, esq).loginEfetuado("rodrigop");
        assertEquals(Optional.of("hugo.pires"), dir2.ler());
        assertEquals("hugo.pires", new PainelLado(LadoCliente.DIREITO, dir2).usuarioLogin());
        assertEquals("rodrigop", new PainelLado(LadoCliente.ESQUERDO, esq).usuarioLogin());
    }

    @Test
    void checkInterceptadorTrocaPorta() {
        PainelLado p = new PainelLado(LadoCliente.DIREITO);
        p.checkInterceptador().doClick();
        assertEquals("127.0.0.1:8002", p.cliente().endereco());
        p.checkInterceptador().doClick();
        assertEquals("127.0.0.1:9002", p.cliente().endereco());
    }
}
