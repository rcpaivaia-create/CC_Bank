package br.com.ccbank.servidor;

import java.util.Locale;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.ccbank.servidor.config.Ambiente;
import br.com.ccbank.servidor.config.ConfiguracaoBanco;
import br.com.ccbank.servidor.negocio.ProcessadorTransacoes;

/** Inicia o servidor de um ambiente: {@code ServidorMain --env=v1|v2}. */
public final class ServidorMain {

    private static final int TAMANHO_POOL = 50;

    private ServidorMain() {
    }

    public static void main(String[] args) throws Exception {
        Ambiente amb = Ambiente.V1;
        for (String arg : args) {
            if (arg.startsWith("--env=")) {
                amb = Ambiente.porNome(arg.substring("--env=".length()));
            }
        }
        // Antes do primeiro uso do SLF4J: o logback.xml usa ccbank.env no nome do arquivo de log.
        System.setProperty("ccbank.env", amb.name().toLowerCase(Locale.ROOT));
        Logger log = LoggerFactory.getLogger(ServidorMain.class);

        ConfiguracaoBanco cfg = ConfiguracaoBanco.carregar(ConfiguracaoBanco.arquivoPadrao());
        DataSource ds = cfg.criarPool(amb.banco(), TAMANHO_POOL);
        ServidorTcp servidor = new ServidorTcp(amb.porta(), new ProcessadorTransacoes(ds), amb.lado());
        servidor.iniciar();
        Runtime.getRuntime().addShutdownHook(new Thread(servidor::parar, "parada-" + amb.lado()));
        log.info("Servidor {} escutando na porta {} (banco {}, {})", amb.lado(), amb.porta(), amb.banco(), cfg);
    }
}
