package br.com.wallet.project.adapter.out.persistence.jpa;

import br.com.wallet.project.adapter.out.persistence.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface JpaTransactionRepository extends JpaRepository<TransactionEntity, Long> {
    @Query("SELECT t FROM TransactionEntity t WHERE t.timestamp >= :startOfDay AND t.timestamp <= :endOfDay AND t.walletEntity.userId = :userId")
    List<TransactionEntity> findTransactionsByDateAndUserId(@Param("startOfDay") LocalDateTime startOfDay,
                                                            @Param("endOfDay") LocalDateTime endOfDay,
                                                            @Param("userId") String userId);

    List<TransactionEntity> findByTransactionTrackId(UUID transactionTrackId);
}

