package br.com.ccbank.testes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import br.com.ccbank.protocolo.Mensagem;
import br.com.ccbank.protocolo.Rotulo;
import br.com.ccbank.servidor.config.Ambiente;
import br.com.ccbank.servidor.negocio.ProcessadorTransacoes;
import br.com.ccbank.servidor.relatorio.ConsultaRelatorio;
import br.com.ccbank.servidor.relatorio.FiltroRelatorio;
import br.com.ccbank.servidor.relatorio.LinhaRelatorio;

class ConsultaRelatorioIT {

    private DataSource ds;
    private ProcessadorTransacoes proc;
    private ConsultaRelatorio consulta;

    private void preparar(Ambiente amb) throws Exception {
        ds = BancoTeste.recriar(amb);
        proc = new ProcessadorTransacoes(ds);
        consulta = new ConsultaRelatorio(ds);
    }

    private static FiltroRelatorio hoje() {
        LocalDate d = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        return new FiltroRelatorio(d, d);
    }

    private void executar(Mensagem m) {
        proc.processar(m, "ip");
    }

    private static Mensagem saqueCom(String operador, String coop, String coopConta, String conta) {
        return Requisicoes.conta(Requisicoes.cabecalho("saque", operador, coop, "001", "001"), coopConta, conta)
                .adicionar(Rotulo.VALOR, "10.00");
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void listaOperacoesDoDia(Ambiente amb) throws Exception {
        preparar(amb);
        executar(Requisicoes.saldo("0001", "100001"));
        executar(Requisicoes.saque("0001", "100001", "10.00"));
        executar(Requisicoes.login("rodrigop", "senha123"));
        List<LinhaRelatorio> linhas = consulta.consultar(hoje());
        assertEquals(Arrays.asList("SALDO", "SAQUE", "LOGIN"),
                Arrays.asList(linhas.get(0).tipo(), linhas.get(1).tipo(), linhas.get(2).tipo()));
        LinhaRelatorio saque = linhas.get(1);
        assertEquals(new BigDecimal("10.00"), saque.valor());
        assertEquals("0001/100001", saque.contaOrigem());
        assertEquals("", saque.contaDestino());
        assertEquals("000", saque.codigoRetorno());
        assertEquals("rodrigop", saque.operador());
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void origemCoerenteNaoDiverge(Ambiente amb) throws Exception {
        preparar(amb);
        executar(Requisicoes.saque("0001", "100001", "10.00"));
        LinhaRelatorio l = consulta.consultar(hoje()).get(0);
        assertFalse(l.divergente());
        assertEquals("0001/001/001", l.terminalCadastrado());
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void origemFalsificadaDiverge(Ambiente amb) throws Exception {
        preparar(amb);
        executar(saqueCom("rodrigop", "0002", "0001", "100001"));
        LinhaRelatorio l = consulta.consultar(hoje()).get(0);
        assertTrue(l.divergente());
        assertEquals("0002", l.cooperativa());
        assertEquals("0001/001/001", l.terminalCadastrado());
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void operadorInexistenteDiverge(Ambiente amb) throws Exception {
        preparar(amb);
        executar(Requisicoes.conta(Requisicoes.cabecalho("saldo", "fantasma", "0001", "001", "001"),
                "0001", "100001"));
        LinhaRelatorio l = consulta.consultar(hoje()).get(0);
        assertTrue(l.divergente());
        assertEquals("operador inexistente", l.terminalCadastrado());
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void loginNuncaDiverge(Ambiente amb) throws Exception {
        preparar(amb);
        executar(Requisicoes.login("naoexiste", "x"));
        LinhaRelatorio l = consulta.consultar(hoje()).get(0);
        assertEquals("LOGIN", l.tipo());
        assertFalse(l.divergente());
        assertEquals("", l.terminalCadastrado());
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void filtros(Ambiente amb) throws Exception {
        preparar(amb);
        executar(saqueCom("rodrigop", "0001", "0001", "100001"));           // coerente
        executar(saqueCom("rodrigop", "0002", "0001", "100002"));           // origem falsificada (e Intercredis)
        executar(Requisicoes.conta(Requisicoes.cabecalho("saldo", "fantasma", "0001", "001", "001"),
                "0001", "100003"));                                  // operador inexistente
        executar(saqueCom("rodrigop", "0001", "0002", "200001"));           // Intercredis coerente
        executar(Requisicoes.validarSenha("0001", "100001", "000000")); // recusada (002)

        assertEquals(5, consulta.consultar(hoje()).size());

        List<LinhaRelatorio> divergentes = consulta.consultar(hoje().somenteDivergencias(true));
        assertEquals(2, divergentes.size());
        assertTrue(divergentes.stream().allMatch(LinhaRelatorio::divergente));

        List<LinhaRelatorio> efetivadas = consulta.consultar(hoje().incluirRecusadas(false));
        assertEquals(4, efetivadas.size());
        assertTrue(efetivadas.stream().allMatch(l -> "000".equals(l.codigoRetorno())));

        List<LinhaRelatorio> intercredis = consulta.consultar(hoje().somenteIntercredis(true));
        assertEquals(2, intercredis.size());
        assertTrue(intercredis.stream().anyMatch(l -> "0002/200001".equals(l.contaOrigem())));

        assertEquals(4, consulta.consultar(hoje().operador("rodrigop")).size());
        assertEquals(3, consulta.consultar(hoje().tipo("SAQUE")).size());
        assertEquals(1, consulta.consultar(hoje().cooperativa("0002")).size());
        assertEquals(5, consulta.consultar(hoje().cooperativa("")).size());
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void foraDoPeriodo(Ambiente amb) throws Exception {
        preparar(amb);
        executar(Requisicoes.saldo("0001", "100001"));
        assertTrue(consulta.consultar(new FiltroRelatorio(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31))).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(Ambiente.class)
    void transferenciaTrazDestino(Ambiente amb) throws Exception {
        preparar(amb);
        executar(Requisicoes.transferencia("0001", "100001", "0002", "200001", "1.00"));
        LinhaRelatorio l = consulta.consultar(hoje()).get(0);
        assertEquals("0001/100001", l.contaOrigem());
        assertEquals("0002/200001", l.contaDestino());
    }
}
