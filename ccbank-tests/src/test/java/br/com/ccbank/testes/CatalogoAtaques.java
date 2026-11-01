package br.com.ccbank.testes;

import java.util.Arrays;
import java.util.List;

/** Os 14 ataques do catálogo (seção 7.3), na ordem. */
final class CatalogoAtaques {

    private CatalogoAtaques() {
    }

    static List<Ataque> todos() {
        return Arrays.asList(
                new ValorAdulterado(),
                new DepositoInflado(),
                new TransferenciaDesviada(),
                new OrigemFalsificada(),
                new OperadorFalsificado(),
                new RespostaFalsa(),
                new SaldoExibidoFalso(),
                new Replay8(),
                new CapturaPassiva(),
                new ValidacaoFalsificada(),
                new OperacaoSemValidacao(),
                new PassTheHash(),
                new QuebraHash(),
                new ChaveAesNoCodigo());
    }
}
