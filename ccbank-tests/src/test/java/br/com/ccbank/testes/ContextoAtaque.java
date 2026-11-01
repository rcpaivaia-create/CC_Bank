package br.com.ccbank.testes;

import java.io.IOException;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.LoggerFactory;

import br.com.ccbank.interceptador.mediacao.Mediador;
import br.com.ccbank.interceptador.mediacao.MediadorPassagem;
import br.com.ccbank.interceptador.mediacao.MediadorRegras;
import br.com.ccbank.interceptador.mediacao.OuvinteTroca;
import br.com.ccbank.interceptador.proxy.ServidorProxy;
import br.com.ccbank.interceptador.regras.MotorRegras;
import br.com.ccbank.interceptador.regras.Regra;
import br.com.ccbank.servidor.ServidorTcp;
import br.com.ccbank.servidor.config.Ambiente;
import br.com.ccbank.servidor.negocio.ProcessadorTransacoes;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/** Contexto de um ataque para um lado: o ambiente, o banco, e como abrir uma sessão pelo proxy. */
final class ContextoAtaque {

    private static final String SUFIXO_LOG = "ataque";

    private final Ambiente ambiente;
    private final DataSource ds;

    ContextoAtaque(Ambiente ambiente, DataSource ds) {
        this.ambiente = ambiente;
        this.ds = ds;
    }

    Ambiente ambiente() {
        return ambiente;
    }

    DataSource ds() {
        return ds;
    }

    /** Sobe servidor + proxy (com {@code regras}, ou passagem se vazio) + cliente através do proxy. */
    SessaoAtaque abrir(List<Regra> regras) throws IOException {
        ServidorTcp servidor = new ServidorTcp(0, new ProcessadorTransacoes(ds), ambiente.lado());
        servidor.iniciar();
        int portaServidor = servidor.portaLocal();

        Mediador mediador = regras.isEmpty() ? new MediadorPassagem() : new MediadorRegras(new MotorRegras(regras));

        Logger loggerLado = (Logger) LoggerFactory.getLogger("ccbank.interceptador." + SUFIXO_LOG);
        loggerLado.setLevel(Level.INFO);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        loggerLado.addAppender(appender);

        ServidorProxy proxy = new ServidorProxy(0, "127.0.0.1", portaServidor, mediador, OuvinteTroca.NULO, SUFIXO_LOG);
        proxy.iniciar();

        ClienteTeste cliente = new ClienteTeste(proxy.portaLocal());
        return new SessaoAtaque(servidor, proxy, cliente, portaServidor, loggerLado, appender);
    }
}
