package br.com.ccbank.testes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;
import br.com.ccbank.servidor.config.Ambiente;
import br.com.ccbank.servidor.negocio.ProcessadorTransacoes;

class SaqueDepositoIT {

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
    void saqueDebitaEGeraLancamento(Ambiente amb) throws Exception {
        preparar(amb);
        Mensagem r = proc.processar(Requisicoes.saque("0001", "100001", "123456", "150.00"), "ip");
        assertEquals(Optional.of("000"), r.primeiro(Rotulo.CODIGO_RETORNO));
        assertEquals(Optional.of("850.00"), r.primeiro(Rotulo.SALDO));
        assertEquals(Optional.of("Maria Souza"), r.primeiro(Rotulo.TITULAR));
        assertEquals(new BigDecimal("850.00"), BancoTeste.saldo(ds, "0001", "100001"));
        assertEquals(1, BancoTeste.contar(ds, "select count(*) from lancamento where natureza='D' and descricao='SAQUE'"
                + " and valor=150.00 and saldo_apos=850.00"));
        assertEquals(1, BancoTeste.contar(ds, "select count(*) from transacao where tipo='SAQUE' and codigo_retorno='000'"
                + " and valor=150.00 and saldo_apos_origem=850.00 and not intercredis"));
        assertEquals(1, BancoTeste.contar(ds, "select count(*) from lancamento l join transacao t on t.id = l.transacao_id"
                + " where t.tipo='SAQUE'"));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void saqueDoSaldoInteiro(Ambiente amb) throws Exception {
        preparar(amb);
        assertEquals(Optional.of("000"), codigo(Requisicoes.saque("0001", "100002", "123456", "500.00")));
        assertEquals(new BigDecimal("0.00"), BancoTeste.saldo(ds, "0001", "100002"));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void saqueUmCentavoAcima(Ambiente amb) throws Exception {
        preparar(amb);
        assertEquals(Optional.of("003"), codigo(Requisicoes.saque("0001", "100002", "123456", "500.01")));
        assertEquals(new BigDecimal("500.00"), BancoTeste.saldo(ds, "0001", "100002"));
        assertEquals(0, BancoTeste.contar(ds, "select count(*) from lancamento"));
        assertEquals(1, BancoTeste.contar(ds, "select count(*) from transacao where tipo='SAQUE' and codigo_retorno='003'"));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void saqueContaZerada(Ambiente amb) throws Exception {
        preparar(amb);
        assertEquals(Optional.of("003"), codigo(Requisicoes.saque("0001", "100010", "123456", "0.01")));
    }

    static Stream<Arguments> valoresInvalidos() {
        return Arrays.stream(Ambiente.values()).flatMap(a -> Stream.of("0", "-1.00", "150,00", "1.001", "abc", "")
                .map(v -> Arguments.of(a, v)));
    }

    @ParameterizedTest
    @MethodSource("valoresInvalidos")
    void valorInvalido(Ambiente amb, String valor) throws Exception {
        preparar(amb);
        assertEquals(Optional.of("005"), codigo(Requisicoes.saque("0001", "100001", "123456", valor)));
        assertEquals(Optional.of("005"), codigo(Requisicoes.deposito("0001", "100001", "123456", valor)));
        assertEquals(new BigDecimal("1000.00"), BancoTeste.saldo(ds, "0001", "100001"));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void depositoCredita(Ambiente amb) throws Exception {
        preparar(amb);
        Mensagem r = proc.processar(Requisicoes.deposito("0001", "100010", "123456", "10.50"), "ip");
        assertEquals(Optional.of("000"), r.primeiro(Rotulo.CODIGO_RETORNO));
        assertEquals(Optional.of("10.50"), r.primeiro(Rotulo.SALDO));
        assertEquals(1, BancoTeste.contar(ds, "select count(*) from lancamento where natureza='C' and descricao='DEPOSITO'"
                + " and valor=10.50 and saldo_apos=10.50"));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void depositoSenhaErrada(Ambiente amb) throws Exception {
        preparar(amb);
        assertEquals(Optional.of("002"), codigo(Requisicoes.deposito("0001", "100010", "111111", "10.00")));
        assertEquals(new BigDecimal("0.00"), BancoTeste.saldo(ds, "0001", "100010"));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void intercredisMarcado(Ambiente amb) throws Exception {
        preparar(amb);
        assertEquals(Optional.of("000"), codigo(Requisicoes.saque("0002", "200001", "123456", "10.00")));
        assertEquals(1, BancoTeste.contar(ds, "select count(*) from transacao where tipo='SAQUE' and intercredis"));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void campoOrigemGiganteNaoQuebra(Ambiente amb) throws Exception {
        preparar(amb);
        StringBuilder gigante = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            gigante.append('9');
        }
        Mensagem m = Requisicoes.conta(Requisicoes.cabecalho("saque", "rodrigop", gigante.toString(), "001", "001"),
                "0001", "100001", "123456").adicionar(Rotulo.VALOR, "10.00");
        assertEquals(Optional.of("000"), codigo(m));
        assertEquals(1, BancoTeste.contar(ds, "select count(*) from transacao where length(cooperativa) = 5000"));
    }
}
