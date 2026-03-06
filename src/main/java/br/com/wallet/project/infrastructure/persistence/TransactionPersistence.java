package br.com.wallet.project.infrastructure.persistence;

import br.com.wallet.project.domain.dto.TransactionDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionPersistence {
    List<TransactionDTO> findTransactionsByDateAndUserId(LocalDateTime startOfDay, LocalDateTime endOfDay, String userId);
    TransactionDTO save(TransactionDTO transactionDTO);
}