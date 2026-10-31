package br.com.ccbank.testes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;
import br.com.ccbank.servidor.config.Ambiente;
import br.com.ccbank.servidor.negocio.ProcessadorTransacoes;

class TransferenciaIT {

    private DataSource ds;
    private ProcessadorTransacoes proc;

    private void preparar(Ambiente amb) throws Exception {
        ds = BancoTeste.recriar(amb);
        proc = new ProcessadorTransacoes(ds);
    }

    private Optional<String> codigo(Mensagem req) {
        return proc.processar(req, "ip").primeiro(Rotulo.CODIGO_RETORNO);
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void transfereEntreCooperativas(Ambiente amb) throws Exception {
        preparar(amb);
        Mensagem r = proc.processar(Requisicoes.transferencia("0001", "100001", "123456", "0002", "200001", "300.00"), "ip");
        assertEquals(Optional.of("000"), r.primeiro(Rotulo.CODIGO_RETORNO));
        assertEquals(Optional.of("700.00"), r.primeiro(Rotulo.SALDO));
        assertEquals(new BigDecimal("700.00"), BancoTeste.saldo(ds, "0001", "100001"));
        assertEquals(new BigDecimal("2300.00"), BancoTeste.saldo(ds, "0002", "200001"));
        assertEquals(1, BancoTeste.contar(ds, "select count(distinct l.transacao_id) from lancamento l"
                + " where (l.descricao='TRANSFERENCIA ENVIADA' and l.natureza='D' and l.saldo_apos=700.00)"
                + " or (l.descricao='TRANSFERENCIA RECEBIDA' and l.natureza='C' and l.saldo_apos=2300.00)"));
        assertEquals(2, BancoTeste.contar(ds, "select count(*) from lancamento"));
        assertEquals(1, BancoTeste.contar(ds, "select count(*) from transacao where tipo='TRANSFERENCIA'"
                + " and conta_destino_id is not null and cooperativa_destino='0002' and conta_destino='200001'"
                + " and saldo_apos_origem=700.00 and saldo_apos_destino=2300.00 and valor=300.00"));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void mesmaConta(Ambiente amb) throws Exception {
        preparar(amb);
        assertEquals(Optional.of("007"), codigo(Requisicoes.transferencia("0001", "100001", "123456", "0001", "100001", "1.00")));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void destinoInexistente(Ambiente amb) throws Exception {
        preparar(amb);
        assertEquals(Optional.of("004"), codigo(Requisicoes.transferencia("0001", "100001", "123456", "0003", "999999", "1.00")));
        assertEquals(new BigDecimal("1000.00"), BancoTeste.saldo(ds, "0001", "100001"));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void saldoInsuficiente(Ambiente amb) throws Exception {
        preparar(amb);
        assertEquals(Optional.of("003"), codigo(Requisicoes.transferencia("0001", "100002", "123456", "0001", "100001", "500.01")));
        assertEquals(0, BancoTeste.contar(ds, "select count(*) from lancamento"));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void senhaErrada(Ambiente amb) throws Exception {
        preparar(amb);
        assertEquals(Optional.of("002"), codigo(Requisicoes.transferencia("0001", "100001", "999999", "0001", "100002", "1.00")));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void transferenciasCruzadasSemDeadlock(Ambiente amb) throws Exception {
        preparar(amb);
        ExecutorService pool = Executors.newFixedThreadPool(20);
        List<Future<Optional<String>>> resultados = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            resultados.add(pool.submit(() -> codigo(Requisicoes.transferencia("0001", "100001", "123456", "0001", "100002", "1.00"))));
            resultados.add(pool.submit(() -> codigo(Requisicoes.transferencia("0001", "100002", "123456", "0001", "100001", "1.00"))));
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "transferências não terminaram em 30 s");
        for (Future<Optional<String>> f : resultados) {
            assertEquals(Optional.of("000"), f.get());
        }
        assertEquals(new BigDecimal("1000.00"), BancoTeste.saldo(ds, "0001", "100001"));
        assertEquals(new BigDecimal("500.00"), BancoTeste.saldo(ds, "0001", "100002"));
        assertEquals(200, BancoTeste.contar(ds, "select count(*) from lancamento"));
    }
}
