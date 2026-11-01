package br.com.ccbank.testes;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import br.com.ccbank.interceptador.proxy.ServidorProxy;
import br.com.ccbank.servidor.ServidorTcp;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/** Uma sessão de ataque aberta: servidor + proxy + cliente através do proxy, com captura de log. */
final class SessaoAtaque implements AutoCloseable {

    private final ServidorTcp servidor;
    private final ServidorProxy proxy;
    private final ClienteTeste cliente;
    private final int portaServidor;
    private final Logger loggerLado;
    private final ListAppender<ILoggingEvent> appender;

    SessaoAtaque(ServidorTcp servidor, ServidorProxy proxy, ClienteTeste cliente, int portaServidor,
            Logger loggerLado, ListAppender<ILoggingEvent> appender) {
        this.servidor = servidor;
        this.proxy = proxy;
        this.cliente = cliente;
        this.portaServidor = portaServidor;
        this.loggerLado = loggerLado;
        this.appender = appender;
    }

    ClienteTeste cliente() {
        return cliente;
    }

    int portaServidor() {
        return portaServidor;
    }

    List<String> logCapturado() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.toList());
    }

    @Override
    public void close() throws IOException {
        loggerLado.detachAppender(appender);
        cliente.close();
        proxy.parar();
        servidor.parar();
    }
}
