package com.integrityfamily.common.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AfterCommit — Unit Tests")
class AfterCommitTest {

    @AfterEach
    void cleanUp() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("Sin transacción activa → ejecuta de inmediato")
    void runsImmediately_whenNoTransaction() {
        AtomicInteger calls = new AtomicInteger();

        AfterCommit.run(calls::incrementAndGet);

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Con transacción activa → no ejecuta hasta el commit y luego ejecuta una sola vez")
    void defersUntilCommit_whenTransactionActive() {
        TransactionSynchronizationManager.initSynchronization();
        AtomicInteger calls = new AtomicInteger();

        AfterCommit.run(calls::incrementAndGet);
        assertThat(calls.get()).as("antes del commit").isZero();

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        assertThat(calls.get()).as("después del commit").isEqualTo(1);
    }

    @Test
    @DisplayName("Rollback → la acción nunca se ejecuta")
    void neverRuns_whenRolledBack() {
        TransactionSynchronizationManager.initSynchronization();
        AtomicInteger calls = new AtomicInteger();

        AfterCommit.run(calls::incrementAndGet);

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(calls.get()).isZero();
    }
}
