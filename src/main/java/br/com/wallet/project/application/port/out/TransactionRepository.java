package br.com.wallet.project.application.port.out;
import br.com.wallet.project.domain.model.Transaction;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
/**
 * Driven port: persistence contract for Transaction.
 */
public interface TransactionRepository {
    List<Transaction> findByDateRangeAndUserId(LocalDateTime start, LocalDateTime end, String userId);
    Transaction save(Transaction transaction);
    List<Transaction> findByTransactionTrackId(UUID transactionTrackId);
}
