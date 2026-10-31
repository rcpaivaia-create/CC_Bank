package br.com.ccbank.testes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

class LoginSaldoIT {

    private DataSource ds;
    private ProcessadorTransacoes proc;

    private void preparar(Ambiente amb) throws Exception {
        ds = BancoTeste.recriar(amb);
        proc = new ProcessadorTransacoes(ds);
    }

    private static Optional<String> codigo(Mensagem resposta) {
        return resposta.primeiro(Rotulo.CODIGO_RETORNO);
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void loginValidoDevolveTerminal(Ambiente amb) throws Exception {
        preparar(amb);
        Mensagem r = proc.processar(Requisicoes.login("rodrigop", "senha123"), "127.0.0.1");
        assertEquals(Optional.of("000"), codigo(r));
        assertEquals(Optional.of("0001"), r.primeiro(Rotulo.COOPERATIVA_OPERADOR));
        assertEquals(Optional.of("001"), r.primeiro(Rotulo.PAC));
        assertEquals(Optional.of("001"), r.primeiro(Rotulo.TERMINAL));
        assertEquals(Optional.of("Rodrigo P."), r.primeiro(Rotulo.NOME_OPERADOR));
        assertEquals(1, BancoTeste.contar(ds, "select count(*) from transacao where tipo='LOGIN' and codigo_retorno='000'"));
    }

    static Stream<Arguments> loginsInvalidos() {
        return Arrays.stream(Ambiente.values()).flatMap(a -> Stream.of(
                Arguments.of(a, "rodrigop", "errada"),
                Arguments.of(a, "Rodrigop", "senha123"),
                Arguments.of(a, "rodrigop ", "senha123"),
                Arguments.of(a, "naoexiste", "senha123")));
    }

    @ParameterizedTest
    @MethodSource("loginsInvalidos")
    void loginInvalido(Ambiente amb, String login, String senha) throws Exception {
        preparar(amb);
        Mensagem r = proc.processar(Requisicoes.login(login, senha), "ip");
        assertEquals(Optional.of("001"), codigo(r));
        assertFalse(r.primeiro(Rotulo.COOPERATIVA_OPERADOR).isPresent());
        assertEquals(1, BancoTeste.contar(ds, "select count(*) from transacao where tipo='LOGIN' and codigo_retorno='001'"));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void operadorInativo(Ambiente amb) throws Exception {
        preparar(amb);
        BancoTeste.executar(ds, "update operador set ativo = false where login = 'rodrigop'");
        assertEquals(Optional.of("001"), codigo(proc.processar(Requisicoes.login("rodrigop", "senha123"), "ip")));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void saldoValido(Ambiente amb) throws Exception {
        preparar(amb);
        Mensagem r = proc.processar(Requisicoes.saldo("0001", "100001"), "ip");
        assertEquals(Optional.of("000"), codigo(r));
        assertEquals(Optional.of("1000.00"), r.primeiro(Rotulo.SALDO));
        assertEquals(Optional.of("Maria Souza"), r.primeiro(Rotulo.TITULAR));
        assertTrue(r.primeiro(Rotulo.ID_TRANSACAO).isPresent());
        assertEquals(1, BancoTeste.contar(ds, "select count(*) from transacao where tipo='SALDO' and codigo_retorno='000'"
                + " and intercredis = false and conta_origem_id is not null"));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void saldoContaInexistente(Ambiente amb) throws Exception {
        preparar(amb);
        assertEquals(Optional.of("004"), codigo(proc.processar(Requisicoes.saldo("0001", "999999"), "ip")));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void campoObrigatorioAusente(Ambiente amb) throws Exception {
        preparar(amb);
        Mensagem semConta = Requisicoes.cabecalho("saldo").adicionar(Rotulo.COOPERATIVA_CONTA, "0001");
        assertEquals(Optional.of("006"), codigo(proc.processar(semConta, "ip")));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void transacaoDesconhecida(Ambiente amb) throws Exception {
        preparar(amb);
        Mensagem r = proc.processar(Requisicoes.cabecalho("pix"), "ip");
        assertEquals(Optional.of("006"), codigo(r));
        assertEquals(1, BancoTeste.contar(ds, "select count(*) from transacao where tipo='INVALIDA' and codigo_retorno='006'"));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void semTransacao(Ambiente amb) throws Exception {
        preparar(amb);
        Mensagem r = proc.processar(new Mensagem().adicionar(Rotulo.OPERADOR, "rodrigop"), "ip");
        assertEquals(Optional.of("006"), codigo(r));
        assertEquals(1, BancoTeste.contar(ds, "select count(*) from transacao where tipo='INVALIDA' and operador_login='rodrigop'"));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void jornalGravaOrigemComoRecebida(Ambiente amb) throws Exception {
        preparar(amb);
        Mensagem m = Requisicoes.conta(Requisicoes.cabecalho("saldo", "fantasma", "9999", "777", "888"),
                "0001", "100001");
        assertEquals(Optional.of("000"), codigo(proc.processar(m, "10.0.0.7")));
        assertEquals(1, BancoTeste.contar(ds, "select count(*) from transacao where operador_login='fantasma'"
                + " and cooperativa='9999' and pac='777' and terminal='888' and intercredis and ip_origem='10.0.0.7'"));
    }

}
