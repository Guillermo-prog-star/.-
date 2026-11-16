package com.integrityfamily.common.event;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Ejecuta una acción después del commit de la transacción en curso (ADR-020).
 *
 * <p>Si no hay sincronización transaccional activa, la ejecuta de inmediato: así
 * es compatible con llamadas sin transacción y con tests unitarios.
 *
 * <p>Si la transacción termina en rollback, la acción nunca se ejecuta.
 */
public final class AfterCommit {

    private AfterCommit() {}

    public static void run(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
