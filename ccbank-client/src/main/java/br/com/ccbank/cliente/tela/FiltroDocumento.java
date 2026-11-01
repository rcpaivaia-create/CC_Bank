package br.com.ccbank.cliente.tela;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** {@link DocumentListener} funcional: o mesmo tratamento nos três eventos do documento. */
@FunctionalInterface
interface FiltroDocumento extends DocumentListener {

    void mudou(DocumentEvent e);

    @Override
    default void insertUpdate(DocumentEvent e) {
        mudou(e);
    }

    @Override
    default void removeUpdate(DocumentEvent e) {
        mudou(e);
    }

    @Override
    default void changedUpdate(DocumentEvent e) {
        mudou(e);
    }
}
