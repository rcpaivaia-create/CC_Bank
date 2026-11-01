package br.com.ccbank.interceptador.mediacao;

import br.com.ccbank.interceptador.regras.MotorRegras;
import br.com.ccbank.interceptador.regras.ResultadoAplicacao;
import br.com.ccbank.interceptador.regras.Sentido;
import br.com.ccbank.protocolo.Mensagem;

/** Aplica o motor de regras da 2a; reserializa só quando alguma regra adulterou a mensagem. */
public final class MediadorRegras implements Mediador {

    private final MotorRegras motor;

    public MediadorRegras(MotorRegras motor) {
        this.motor = motor;
    }

    @Override
    public DecisaoMediacao intervir(Mensagem mensagem, Sentido sentido) {
        ResultadoAplicacao resultado = motor.aplicar(mensagem, sentido);
        return resultado.houveAdulteracao()
                ? DecisaoMediacao.encaminharModificada(resultado)
                : DecisaoMediacao.encaminharOriginal(mensagem);
    }
}
