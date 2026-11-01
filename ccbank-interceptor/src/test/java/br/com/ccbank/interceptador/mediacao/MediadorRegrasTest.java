package br.com.ccbank.interceptador.mediacao;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import br.com.ccbank.interceptador.regras.Acao;
import br.com.ccbank.interceptador.regras.Condicao;
import br.com.ccbank.interceptador.regras.MotorRegras;
import br.com.ccbank.interceptador.regras.Regra;
import br.com.ccbank.interceptador.regras.Sentido;
import br.com.ccbank.protocolo.Mensagem;

class MediadorRegrasTest {

    private static MotorRegras motorSaqueCentavos() {
        return new MotorRegras(singletonList(new Regra("x", true, Sentido.REQUISICAO,
                new Condicao(singletonMap("00001", "saque")),
                singletonList(new Acao(Acao.Tipo.SET, "00030", "0.01")))));
    }

    @Test
    void comRegraQueCasaReserializa() {
        DecisaoMediacao d = new MediadorRegras(motorSaqueCentavos()).intervir(
                new Mensagem().adicionar("00001", "saque").adicionar("00030", "500.00"), Sentido.REQUISICAO);
        assertTrue(d.reserializar());
        assertEquals(Optional.of("0.01"), d.resultado().mensagem().primeiro("00030"));
    }

    @Test
    void semRegraQueCasaNaoReserializa() {
        DecisaoMediacao d = new MediadorRegras(motorSaqueCentavos()).intervir(
                new Mensagem().adicionar("00001", "deposito").adicionar("00030", "500.00"), Sentido.REQUISICAO);
        assertEquals(DecisaoMediacao.Acao.ENCAMINHAR, d.acao());
        assertFalse(d.reserializar());
    }
}
