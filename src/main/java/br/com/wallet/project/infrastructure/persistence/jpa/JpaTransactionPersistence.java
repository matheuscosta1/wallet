package br.com.wallet.project.infrastructure.persistence.jpa;

import br.com.wallet.project.domain.model.Transaction;
import br.com.wallet.project.infrastructure.persistence.TransactionPersistence;
import br.com.wallet.project.infrastructure.persistence.jpa.repository.JpaTransactionRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class JpaTransactionPersistence implements TransactionPersistence {
    private final JpaTransactionRepository jpaTransactionRepository;

    public JpaTransactionPersistence(JpaTransactionRepository jpaTransactionRepository) {
        this.jpaTransactionRepository = jpaTransactionRepository;
    }

    @Override
    public List<Transaction> findTransactionsByDateAndUserId(LocalDateTime startOfDay, LocalDateTime endOfDay, String userId) {
        return jpaTransactionRepository.findTransactionsByDateAndUserId(startOfDay, endOfDay, userId);
    }

    @Override
    public Transaction save(Transaction transaction) {
        return jpaTransactionRepository.save(transaction);
    }
}