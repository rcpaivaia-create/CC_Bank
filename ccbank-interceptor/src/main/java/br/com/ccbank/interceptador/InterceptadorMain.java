package br.com.ccbank.interceptador;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.ccbank.interceptador.config.LadoInterceptador;
import br.com.ccbank.interceptador.proxy.ServidorProxy;
import br.com.ccbank.interceptador.regras.CarregadorRegras;
import br.com.ccbank.interceptador.regras.MotorRegras;
import br.com.ccbank.interceptador.regras.Regra;

/**
 * Inicia o interceptador de um lado, sem tela:
 * {@code InterceptadorMain --headless --lado=esquerdo|direito [--regras=<arquivo.json>]}.
 * A tela chega na fase 2b.
 */
public final class InterceptadorMain {

    private InterceptadorMain() {
    }

    public static void main(String[] args) {
        int codigo = executar(args);
        if (codigo != 0) {
            System.exit(codigo);
        }
    }

    /**
     * Sobe o proxy e retorna sem bloquear (a thread aceitadora mantém a JVM viva).
     *
     * @return 0 se subiu; 1 se falhou, com o motivo no log e no console
     */
    static int executar(String[] args) {
        try {
            boolean headless = false;
            String ladoArg = null;
            String regrasArg = null;
            for (String arg : args) {
                if (arg.equals("--headless")) {
                    headless = true;
                } else if (arg.startsWith("--lado=")) {
                    ladoArg = arg.substring("--lado=".length());
                } else if (arg.startsWith("--regras=")) {
                    regrasArg = arg.substring("--regras=".length());
                }
            }
            if (!headless) {
                throw new IllegalArgumentException(
                        "uso: --headless --lado=esquerdo|direito [--regras=<arquivo.json>] (a tela chega na 2b)");
            }
            if (ladoArg == null) {
                throw new IllegalArgumentException("informe --lado=esquerdo|direito");
            }
            LadoInterceptador lado = LadoInterceptador.porNome(ladoArg);

            // Antes do primeiro uso do SLF4J: o logback.xml usa ccbank.lado no nome do arquivo de log.
            System.setProperty("ccbank.lado", lado.sufixoLog());
            Logger log = LoggerFactory.getLogger(InterceptadorMain.class);

            List<Regra> regras = regrasArg == null
                    ? Collections.<Regra>emptyList()
                    : new ArrayList<>(CarregadorRegras.carregar(Paths.get(regrasArg)));
            MotorRegras motor = new MotorRegras(regras);

            ServidorProxy proxy = ServidorProxy.doLado(lado, motor);
            proxy.iniciar();
            Runtime.getRuntime().addShutdownHook(new Thread(proxy::parar, "parada-proxy-" + lado.sufixoLog()));

            String modo = regrasArg == null ? "passagem" : "regras";
            log.info("Interceptador {} escutando na porta {} -> {}:{} (modo {}, {} regras)",
                    lado.sufixoLog(), lado.portaEscuta(), lado.hostDestino(), lado.portaDestino(), modo, regras.size());
            return 0;
        } catch (Exception e) {
            LoggerFactory.getLogger(InterceptadorMain.class)
                    .error("Falha ao iniciar o interceptador: {}", e.getMessage(), e);
            return 1;
        }
    }
}
