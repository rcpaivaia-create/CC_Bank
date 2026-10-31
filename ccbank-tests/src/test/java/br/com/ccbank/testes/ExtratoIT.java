package br.com.ccbank.testes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
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

class ExtratoIT {

    private DataSource ds;
    private ProcessadorTransacoes proc;

    private void preparar(Ambiente amb) throws Exception {
        ds = BancoTeste.recriar(amb);
        proc = new ProcessadorTransacoes(ds);
    }

    private Mensagem executar(Mensagem req) {
        Mensagem r = proc.processar(req, "ip");
        return r;
    }

    private static List<String> todos(Mensagem m, Rotulo r) {
        return m.todos(r.codigo());
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void extratoSemDatasTrazUltimos30Dias(Ambiente amb) throws Exception {
        preparar(amb);
        executar(Requisicoes.deposito("0001", "100001", "123456", "10.00"));
        executar(Requisicoes.saque("0001", "100001", "123456", "5.00"));
        executar(Requisicoes.transferencia("0001", "100001", "123456", "0002", "200001", "1.00"));

        Mensagem r = executar(Requisicoes.extrato("0001", "100001", "123456", null, null));
        assertEquals(Optional.of("000"), r.primeiro(Rotulo.CODIGO_RETORNO));
        assertEquals(Optional.of("Maria Souza"), r.primeiro(Rotulo.TITULAR));
        assertEquals(Optional.of("3"), r.primeiro(Rotulo.QTD_LANCAMENTOS));
        assertEquals(3, todos(r, Rotulo.LANC_DATA_HORA).size());
        assertEquals(Arrays.asList("DEPOSITO", "SAQUE", "TRANSFERENCIA ENVIADA"), todos(r, Rotulo.LANC_TIPO));
        assertEquals(Arrays.asList("10.00", "-5.00", "-1.00"), todos(r, Rotulo.LANC_VALOR));
        assertEquals(Arrays.asList("1010.00", "1005.00", "1004.00"), todos(r, Rotulo.LANC_SALDO_APOS));
        assertEquals(Arrays.asList("", "", "0002/200001"), todos(r, Rotulo.LANC_CONTRAPARTE));
        assertTrue(r.primeiro(Rotulo.LANC_DATA_HORA).get().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void contraparteDoLadoQueRecebe(Ambiente amb) throws Exception {
        preparar(amb);
        executar(Requisicoes.transferencia("0001", "100001", "123456", "0002", "200001", "1.00"));
        Mensagem r = executar(Requisicoes.extrato("0002", "200001", "123456", null, null));
        assertEquals(Arrays.asList("TRANSFERENCIA RECEBIDA"), todos(r, Rotulo.LANC_TIPO));
        assertEquals(Arrays.asList("0001/100001"), todos(r, Rotulo.LANC_CONTRAPARTE));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void extratoForaDoPeriodo(Ambiente amb) throws Exception {
        preparar(amb);
        executar(Requisicoes.deposito("0001", "100001", "123456", "10.00"));
        Mensagem r = executar(Requisicoes.extrato("0001", "100001", "123456", "2020-01-01", "2020-01-31"));
        assertEquals(Optional.of("000"), r.primeiro(Rotulo.CODIGO_RETORNO));
        assertEquals(Optional.of("0"), r.primeiro(Rotulo.QTD_LANCAMENTOS));
        assertEquals(0, todos(r, Rotulo.LANC_DATA_HORA).size());
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void limitaA200MaisRecentes(Ambiente amb) throws Exception {
        preparar(amb);
        for (int i = 0; i < 205; i++) {
            executar(Requisicoes.deposito("0001", "100001", "123456", "1.00"));
        }
        Mensagem r = executar(Requisicoes.extrato("0001", "100001", "123456", null, null));
        assertEquals(Optional.of("200"), r.primeiro(Rotulo.QTD_LANCAMENTOS));
        List<String> saldos = todos(r, Rotulo.LANC_SALDO_APOS);
        assertEquals(200, saldos.size());
        assertEquals("1006.00", saldos.get(0));
        assertEquals("1205.00", saldos.get(199));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void saldosEncadeadosAposOperacoesConcorrentes(Ambiente amb) throws Exception {
        preparar(amb);
        ExecutorService pool = Executors.newFixedThreadPool(20);
        List<Future<Mensagem>> resultados = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            resultados.add(pool.submit(() -> executar(Requisicoes.deposito("0001", "100001", "123456", "1.00"))));
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS));
        for (Future<Mensagem> f : resultados) {
            assertEquals(Optional.of("000"), f.get().primeiro(Rotulo.CODIGO_RETORNO));
        }
        Mensagem r = executar(Requisicoes.extrato("0001", "100001", "123456", null, null));
        List<String> valores = todos(r, Rotulo.LANC_VALOR);
        List<String> saldos = todos(r, Rotulo.LANC_SALDO_APOS);
        for (int i = 1; i < saldos.size(); i++) {
            BigDecimal esperado = new BigDecimal(saldos.get(i - 1)).add(new BigDecimal(valores.get(i)));
            assertEquals(esperado, new BigDecimal(saldos.get(i)), "lançamento " + i + " fora de ordem");
        }
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void dataMalformada(Ambiente amb) throws Exception {
        preparar(amb);
        Mensagem r = executar(Requisicoes.extrato("0001", "100001", "123456", "31/10/2026", null));
        assertEquals(Optional.of("006"), r.primeiro(Rotulo.CODIGO_RETORNO));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void inicioDepoisDoFim(Ambiente amb) throws Exception {
        preparar(amb);
        Mensagem r = executar(Requisicoes.extrato("0001", "100001", "123456", "2026-10-31", "2026-10-01"));
        assertEquals(Optional.of("006"), r.primeiro(Rotulo.CODIGO_RETORNO));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void extratoNaoGeraLancamento(Ambiente amb) throws Exception {
        preparar(amb);
        executar(Requisicoes.extrato("0001", "100001", "123456", null, null));
        assertEquals(0, BancoTeste.contar(ds, "select count(*) from lancamento"));
        assertEquals(1, BancoTeste.contar(ds, "select count(*) from transacao where tipo='EXTRATO' and codigo_retorno='000'"));
    }
}
