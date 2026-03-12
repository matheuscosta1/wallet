package br.com.wallet.project.adapter.out.persistence;
import br.com.wallet.project.adapter.out.persistence.jpa.JpaTransactionRepository;
import br.com.wallet.project.application.port.out.TransactionRepository;
import br.com.wallet.project.domain.model.Transaction;
import br.com.wallet.project.shared.mapper.TransactionEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
/** Driven adapter: implements TransactionRepository port using JPA + PostgreSQL. */
@Repository
@RequiredArgsConstructor
public class TransactionPersistenceAdapter implements TransactionRepository {
    private final JpaTransactionRepository jpaTransactionRepository;
    @Override
    public List<Transaction> findByDateRangeAndUserId(LocalDateTime start, LocalDateTime end, String userId) {
        return jpaTransactionRepository.findTransactionsByDateAndUserId(start, end, userId)
            .stream().map(TransactionEntityMapper::toDomain).collect(Collectors.toList());
    }
    @Override
    public Transaction save(Transaction transaction) {
        return TransactionEntityMapper.toDomain(jpaTransactionRepository.save(TransactionEntityMapper.toEntity(transaction)));
    }
    @Override
    public List<Transaction> findByTransactionTrackId(UUID transactionTrackId) {
        return jpaTransactionRepository.findByTransactionTrackId(transactionTrackId)
            .stream().map(TransactionEntityMapper::toDomain).collect(Collectors.toList());
    }
}
