package br.com.ccbank.testes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;
import br.com.ccbank.servidor.ServidorTcp;
import br.com.ccbank.servidor.config.Ambiente;
import br.com.ccbank.servidor.negocio.ProcessadorTransacoes;

class ServidorTcpIT {

    private DataSource ds;
    private ServidorTcp servidor;

    private int iniciar(Ambiente amb) throws Exception {
        ds = BancoTeste.recriar(amb);
        servidor = new ServidorTcp(0, new ProcessadorTransacoes(ds), amb.lado());
        servidor.iniciar();
        return servidor.portaLocal();
    }

    @AfterEach
    void parar() {
        if (servidor != null) {
            servidor.parar();
        }
    }

    private static Optional<String> codigo(Mensagem resposta) {
        return resposta.primeiro(Rotulo.CODIGO_RETORNO);
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void loginESaldoPelaRede(Ambiente amb) throws Exception {
        int porta = iniciar(amb);
        try (ClienteTeste c = new ClienteTeste(porta)) {
            assertEquals(Optional.of("000"), codigo(c.enviar(Requisicoes.login("rodrigop", "senha123"))));
            Mensagem saldo = c.enviar(Requisicoes.saldo("0001", "100001", "123456"));
            assertEquals(Optional.of("000"), codigo(saldo));
            assertEquals(Optional.of("1000.00"), saldo.primeiro(Rotulo.SALDO));
        }
        assertEquals(1, BancoTeste.contar(ds, "select count(*) from transacao where tipo='SALDO' and ip_origem='127.0.0.1'"));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void mensagemMalformadaResponde006EContinua(Ambiente amb) throws Exception {
        int porta = iniciar(amb);
        try (ClienteTeste c = new ClienteTeste(porta)) {
            c.enviarBytes("lixo\n\n".getBytes(StandardCharsets.UTF_8));
            assertEquals(Optional.of("006"), codigo(c.receber()));
            assertEquals(Optional.of("000"), codigo(c.enviar(Requisicoes.saldo("0001", "100001", "123456"))));
        }
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void clienteQueCaiNoMeioNaoDerrubaServidor(Ambiente amb) throws Exception {
        int porta = iniciar(amb);
        try (ClienteTeste c = new ClienteTeste(porta)) {
            c.enviarBytes("00001=sal".getBytes(StandardCharsets.UTF_8));
        }
        try (ClienteTeste c = new ClienteTeste(porta)) {
            assertEquals(Optional.of("000"), codigo(c.enviar(Requisicoes.saldo("0001", "100001", "123456"))));
        }
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void mensagemGrandeFechaConexao(Ambiente amb) throws Exception {
        int porta = iniciar(amb);
        try (ClienteTeste c = new ClienteTeste(porta)) {
            byte[] grande = new byte[70_000];
            Arrays.fill(grande, (byte) 'a');
            try {
                c.enviarBytes(grande);
            } catch (IOException servidorJaFechou) {
                // aceitável: o servidor pode fechar antes de o cliente terminar de escrever
            }
            assertTrue(c.conexaoFechada());
        }
        try (ClienteTeste c = new ClienteTeste(porta)) {
            assertEquals(Optional.of("000"), codigo(c.enviar(Requisicoes.saldo("0001", "100001", "123456"))));
        }
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void cemSaquesConcorrentesNaMesmaConta(Ambiente amb) throws Exception {
        int porta = iniciar(amb);
        BigDecimal somaInicial = somaSaldos();
        ExecutorService pool = Executors.newFixedThreadPool(100);
        List<Future<Optional<String>>> resultados = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            resultados.add(pool.submit(() -> {
                try (ClienteTeste c = new ClienteTeste(porta)) {
                    return codigo(c.enviar(Requisicoes.saque("0001", "100002", "123456", "10.00")));
                }
            }));
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS), "saques não terminaram em 60 s");
        int sucesso = 0;
        int insuficiente = 0;
        for (Future<Optional<String>> f : resultados) {
            String cod = f.get().orElse("ausente");
            if ("000".equals(cod)) {
                sucesso++;
            } else if ("003".equals(cod)) {
                insuficiente++;
            }
        }
        assertEquals(50, sucesso);
        assertEquals(50, insuficiente);
        assertEquals(new BigDecimal("0.00"), BancoTeste.saldo(ds, "0001", "100002"));
        assertEquals(50, BancoTeste.contar(ds, "select count(*) from lancamento"));
        assertEquals(100, BancoTeste.contar(ds, "select count(*) from transacao where tipo='SAQUE'"));
        assertEquals(somaInicial.subtract(new BigDecimal("500.00")), somaSaldos());
    }

    private BigDecimal somaSaldos() throws Exception {
        try (Connection c = ds.getConnection();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery("select sum(saldo) from conta")) {
            rs.next();
            return rs.getBigDecimal(1);
        }
    }
}
