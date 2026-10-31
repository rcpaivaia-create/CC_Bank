package br.com.ccbank.testes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import br.com.ccbank.cliente.ClienteBanco;
import br.com.ccbank.cliente.ConexaoServidor;
import br.com.ccbank.cliente.LadoCliente;
import br.com.ccbank.cliente.LinhaExtrato;
import br.com.ccbank.cliente.Resposta;
import br.com.ccbank.cliente.ServicoIndisponivelException;
import br.com.ccbank.protocolo.Rotulo;
import br.com.ccbank.servidor.ServidorTcp;
import br.com.ccbank.servidor.config.Ambiente;
import br.com.ccbank.servidor.negocio.ProcessadorTransacoes;

class ClienteBancoIT {

    private DataSource ds;
    private ServidorTcp servidor;
    private ClienteBanco cliente;

    private void iniciar(Ambiente amb) throws Exception {
        ds = BancoTeste.recriar(amb);
        servidor = new ServidorTcp(0, new ProcessadorTransacoes(ds), amb.lado());
        servidor.iniciar();
        cliente = new ClienteBanco(LadoCliente.ESQUERDO,
                new ConexaoServidor(LadoCliente.ESQUERDO, "127.0.0.1", servidor.portaLocal()));
    }

    private void logar() throws IOException {
        assertTrue(cliente.login("rodrigop", "senha123").sucesso());
    }

    @AfterEach
    void parar() {
        if (cliente != null) {
            cliente.sair();
        }
        if (servidor != null) {
            servidor.parar();
        }
    }

    private static int portaLivre() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void loginGuardaOperador(Ambiente amb) throws Exception {
        iniciar(amb);
        Resposta r = cliente.login("rodrigop", "senha123");
        assertTrue(r.sucesso());
        assertTrue(cliente.logado());
        assertEquals("rodrigop", cliente.operador());
        assertEquals("0001", cliente.cooperativa());
        assertEquals("001", cliente.pac());
        assertEquals("001", cliente.terminal());
        assertEquals("Rodrigo P.", cliente.nomeOperador());
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void loginInvalidoNaoLoga(Ambiente amb) throws Exception {
        iniciar(amb);
        Resposta r = cliente.login("rodrigop", "x");
        assertEquals("001", r.codigo());
        assertFalse(r.sucesso());
        assertFalse(cliente.logado());
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void operacaoSemLoginFalha(Ambiente amb) throws Exception {
        iniciar(amb);
        assertThrows(IllegalStateException.class, () -> cliente.saldo("0001", "100001", "123456"));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void saqueComValorBrasileiro(Ambiente amb) throws Exception {
        iniciar(amb);
        logar();
        Resposta r = cliente.saque("0001", "100001", "123456", "1.000,00");
        assertTrue(r.sucesso(), r.texto());
        assertEquals(Optional.of("0.00"), r.mensagem().primeiro(Rotulo.SALDO));
        assertEquals(Optional.of("1000.00"), r.requisicao().primeiro(Rotulo.VALOR));
        assertEquals(Optional.of("0001"), r.requisicao().primeiro(Rotulo.COOPERATIVA_OPERADOR));
        assertTrue(r.duracaoMs() >= 0);
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void extratoAgrupaLinhas(Ambiente amb) throws Exception {
        iniciar(amb);
        logar();
        assertTrue(cliente.deposito("0001", "100001", "123456", "10,00").sucesso());
        Resposta r = cliente.extrato("0001", "100001", "123456", "", "");
        assertTrue(r.sucesso(), r.texto());
        List<LinhaExtrato> linhas = ClienteBanco.linhasExtrato(r.mensagem());
        assertEquals(1, linhas.size());
        assertEquals("DEPOSITO", linhas.get(0).tipo());
        assertEquals("10.00", linhas.get(0).valor());
        assertEquals("1010.00", linhas.get(0).saldoApos());
        assertEquals("", linhas.get(0).contraparte());
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void extratoComDataBrasileira(Ambiente amb) throws Exception {
        iniciar(amb);
        logar();
        Resposta r = cliente.extrato("0001", "100001", "123456", "01/01/2020", "31/01/2020");
        assertTrue(r.sucesso(), r.texto());
        assertEquals(Optional.of("2020-01-01"), r.requisicao().primeiro(Rotulo.DATA_INICIO));
        assertEquals(0, ClienteBanco.linhasExtrato(r.mensagem()).size());
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void transferenciaPeloCliente(Ambiente amb) throws Exception {
        iniciar(amb);
        logar();
        Resposta r = cliente.transferencia("0001", "100001", "123456", "0002", "200001", "10,00");
        assertTrue(r.sucesso(), r.texto());
        assertEquals(Optional.of("990.00"), r.mensagem().primeiro(Rotulo.SALDO));
    }

    @Test
    void servidorForaDoArDaMensagemClara() throws Exception {
        int porta = portaLivre();
        ConexaoServidor c = new ConexaoServidor(LadoCliente.ESQUERDO, "127.0.0.1", porta);
        ServicoIndisponivelException e = assertThrows(ServicoIndisponivelException.class,
                () -> new ClienteBanco(LadoCliente.ESQUERDO, c).login("rodrigop", "senha123"));
        assertEquals("Servidor não disponível em 127.0.0.1:" + porta, e.getMessage());
    }

    @Test
    void portaDeInterceptadorDaMensagemDeInterceptador() {
        ConexaoServidor c = new ConexaoServidor(LadoCliente.ESQUERDO, "127.0.0.1", 8001);
        ServicoIndisponivelException e = assertThrows(ServicoIndisponivelException.class,
                () -> new ClienteBanco(LadoCliente.ESQUERDO, c).login("rodrigop", "senha123"));
        assertEquals("Interceptador não disponível em 127.0.0.1:8001", e.getMessage());
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void reconectaAposServidorReiniciar(Ambiente amb) throws Exception {
        iniciar(amb);
        logar();
        int porta = servidor.portaLocal();
        servidor.parar();
        servidor = new ServidorTcp(porta, new ProcessadorTransacoes(ds), amb.lado());
        servidor.iniciar();
        assertThrows(IOException.class, () -> cliente.saldo("0001", "100001", "123456"));
        assertTrue(cliente.saldo("0001", "100001", "123456").sucesso());
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void chamadasConcorrentesNaoSeMisturam(Ambiente amb) throws Exception {
        iniciar(amb);
        logar();
        List<String[]> contas = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            contas.add(new String[] {"0001", String.format("1000%02d", i)});
            contas.add(new String[] {"0002", String.format("2000%02d", i)});
        }
        ExecutorService pool = Executors.newFixedThreadPool(20);
        List<Future<Resposta>> respostas = new ArrayList<>();
        for (String[] conta : contas) {
            respostas.add(pool.submit(() -> cliente.saldo(conta[0], conta[1], "123456")));
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
        for (int i = 0; i < contas.size(); i++) {
            String[] conta = contas.get(i);
            assertEquals(Optional.of(BancoTeste.titular(ds, conta[0], conta[1])),
                    respostas.get(i).get().mensagem().primeiro(Rotulo.TITULAR), conta[0] + "/" + conta[1]);
        }
    }
}
