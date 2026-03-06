package br.com.wallet.project.infrastructure.persistence.jpa;

import br.com.wallet.project.domain.dto.TransactionDTO;
import br.com.wallet.project.domain.model.TransactionEntity;
import br.com.wallet.project.infrastructure.persistence.TransactionRepository;
import br.com.wallet.project.infrastructure.persistence.jpa.repository.JpaTransactionRepository;
import br.com.wallet.project.mapper.TransactionDomainMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class TransactionRepositoryJpaAdapter implements TransactionRepository {
    private final JpaTransactionRepository jpaTransactionRepository;

    public TransactionRepositoryJpaAdapter(JpaTransactionRepository jpaTransactionRepository) {
        this.jpaTransactionRepository = jpaTransactionRepository;
    }

    @Override
    public List<TransactionDTO> findTransactionsByDateAndUserId(LocalDateTime startOfDay, LocalDateTime endOfDay, String userId) {
        return jpaTransactionRepository.findTransactionsByDateAndUserId(startOfDay, endOfDay, userId)
                .stream()
                .map(TransactionDomainMapper::toTransactionDomain)
                .collect(Collectors.toList());
    }

    @Override
    public TransactionDTO save(TransactionDTO transactionDTO) {
        TransactionEntity entity = TransactionDomainMapper.toTransactionEntity(transactionDTO);
        TransactionEntity saved = jpaTransactionRepository.save(entity);
        return TransactionDomainMapper.toTransactionDomain(saved);
    }
}