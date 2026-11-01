package br.com.ccbank.interceptador.mediacao;

import java.util.List;

import br.com.ccbank.interceptador.regras.MotorRegras;
import br.com.ccbank.interceptador.regras.Regra;
import br.com.ccbank.interceptador.regras.Sentido;
import br.com.ccbank.protocolo.Mensagem;

/**
 * Mediador de uma aba: delega ao mediador do modo atual (passagem, manual ou regras),
 * trocável em runtime. Começa em {@link ModoInterceptador#PASSAGEM}.
 */
public final class MediadorModo implements Mediador {

    private final MediadorPassagem passagem = new MediadorPassagem();
    private final ControladorManual manual;
    private volatile MediadorRegras regras = new MediadorRegras(new MotorRegras(java.util.Collections.emptyList()));
    private volatile ModoInterceptador modo = ModoInterceptador.PASSAGEM;

    public MediadorModo(ControladorManual manual) {
        this.manual = manual;
    }

    public void modo(ModoInterceptador modo) {
        this.modo = modo;
    }

    public ModoInterceptador modo() {
        return modo;
    }

    public void carregarRegras(List<Regra> regras) {
        this.regras = new MediadorRegras(new MotorRegras(regras));
    }

    public ControladorManual controladorManual() {
        return manual;
    }

    @Override
    public DecisaoMediacao intervir(Mensagem mensagem, Sentido sentido) {
        switch (modo) {
            case MANUAL:
                return manual.intervir(mensagem, sentido);
            case REGRAS:
                return regras.intervir(mensagem, sentido);
            case PASSAGEM:
            default:
                return passagem.intervir(mensagem, sentido);
        }
    }
}
