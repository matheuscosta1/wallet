package br.com.wallet.project.repositoy;

import br.com.wallet.project.repositoy.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Query("SELECT t FROM Transaction t WHERE t.timestamp >= :startOfDay AND t.timestamp <= :endOfDay AND t.wallet.userId = :userId")
    List<Transaction> findTransactionsByDateAndUserId(@Param("startOfDay") LocalDateTime startOfDay,
                                                      @Param("endOfDay") LocalDateTime endOfDay,
                                                      @Param("userId") String userId);

    List<Transaction> findByTransactionTrackId(UUID transactionTrackId);
}

