package br.com.ccbank.cliente.tela;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.SwingWorker;

/** Executa um trabalho fora da EDT e entrega o resultado (ou a falha) de volta na EDT. */
final class Tarefa {

    private Tarefa() {
    }

    static <T> void executar(Callable<T> trabalho, Consumer<T> sucesso, Consumer<Throwable> falha, Runnable sempre) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return trabalho.call();
            }

            @Override
            protected void done() {
                try {
                    sucesso.accept(get());
                } catch (ExecutionException e) {
                    falha.accept(e.getCause() != null ? e.getCause() : e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    falha.accept(e);
                } finally {
                    sempre.run();
                }
            }
        }.execute();
    }
}
