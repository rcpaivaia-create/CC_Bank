package br.com.ccbank.testes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;
import br.com.ccbank.servidor.config.Ambiente;
import br.com.ccbank.servidor.negocio.ProcessadorTransacoes;

class ValidarSenhaIT {

    private DataSource ds;
    private ProcessadorTransacoes proc;

    private void preparar(Ambiente amb) throws Exception {
        ds = BancoTeste.recriar(amb);
        proc = new ProcessadorTransacoes(ds);
    }

    private Mensagem executar(Mensagem req) {
        return proc.processar(req, "ip");
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void senhaCorreta(Ambiente amb) throws Exception {
        preparar(amb);
        Mensagem r = executar(Requisicoes.validarSenha("0001", "100001", "123456"));
        assertEquals(Optional.of("000"), r.primeiro(Rotulo.CODIGO_RETORNO));
        assertEquals(Optional.of("Senha validada"), r.primeiro(Rotulo.MENSAGEM_RETORNO));
        assertEquals(Optional.of("Maria Souza"), r.primeiro(Rotulo.TITULAR));
        assertTrue(r.primeiro(Rotulo.ID_TRANSACAO).isPresent());
        assertEquals(1, BancoTeste.contar(ds, "select count(*) from transacao where tipo='VALIDAR_SENHA'"
                + " and codigo_retorno='000' and conta_origem_id is not null and not intercredis"));
        assertEquals(0, BancoTeste.contar(ds, "select count(*) from lancamento"));
        assertEquals(new BigDecimal("1000.00"), BancoTeste.saldo(ds, "0001", "100001"));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void senhaErrada(Ambiente amb) throws Exception {
        preparar(amb);
        Mensagem r = executar(Requisicoes.validarSenha("0001", "100001", "654321"));
        assertEquals(Optional.of("002"), r.primeiro(Rotulo.CODIGO_RETORNO));
        assertFalse(r.primeiro(Rotulo.TITULAR).isPresent());
        assertTrue(r.primeiro(Rotulo.ID_TRANSACAO).isPresent());
        assertEquals(1, BancoTeste.contar(ds, "select count(*) from transacao where tipo='VALIDAR_SENHA' and codigo_retorno='002'"));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void contaInexistente(Ambiente amb) throws Exception {
        preparar(amb);
        Mensagem r = executar(Requisicoes.validarSenha("0001", "999999", "123456"));
        assertEquals(Optional.of("004"), r.primeiro(Rotulo.CODIGO_RETORNO));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void semSenha(Ambiente amb) throws Exception {
        preparar(amb);
        Mensagem r = executar(Requisicoes.conta(Requisicoes.cabecalho("validar_senha"), "0001", "100001"));
        assertEquals(Optional.of("006"), r.primeiro(Rotulo.CODIGO_RETORNO));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void intercredisNaValidacao(Ambiente amb) throws Exception {
        preparar(amb);
        executar(Requisicoes.validarSenha("0002", "200001", "123456"));
        assertEquals(1, BancoTeste.contar(ds, "select count(*) from transacao where tipo='VALIDAR_SENHA' and intercredis"));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void operacaoSemValidacaoEhAceitaNoV1(Ambiente amb) throws Exception {
        preparar(amb);
        Mensagem r = executar(Requisicoes.saque("0001", "100001", "10.00"));
        assertEquals(Optional.of("000"), r.primeiro(Rotulo.CODIGO_RETORNO));
        assertEquals(new BigDecimal("990.00"), BancoTeste.saldo(ds, "0001", "100001"));
        assertEquals(0, BancoTeste.contar(ds, "select count(*) from transacao where tipo='VALIDAR_SENHA'"));
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void senhaNaOperacaoEhIgnorada(Ambiente amb) throws Exception {
        preparar(amb);
        Mensagem m = Requisicoes.saque("0001", "100001", "10.00").adicionar(Rotulo.SENHA_CONTA, "000000");
        assertEquals(Optional.of("000"), executar(m).primeiro(Rotulo.CODIGO_RETORNO));
    }
}
