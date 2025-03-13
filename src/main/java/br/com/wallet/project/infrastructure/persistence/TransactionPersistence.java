package br.com.wallet.project.infrastructure.persistence;

import br.com.wallet.project.domain.model.Transaction;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionPersistence {
    List<Transaction> findTransactionsByDateAndUserId(LocalDateTime startOfDay, LocalDateTime endOfDay, String userId);
    Transaction save(Transaction transaction);
}