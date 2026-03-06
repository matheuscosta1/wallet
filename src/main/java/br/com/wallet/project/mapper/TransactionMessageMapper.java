package br.com.wallet.project.mapper;

import br.com.wallet.project.domain.TransactionMessage;
import br.com.wallet.project.domain.TransactionType;

import java.math.BigDecimal;
import java.util.UUID;

public class TransactionMessageMapper {
    public static TransactionMessage mapTransactionMessage(String userId, UUID transactionId, TransactionType transactionType, BigDecimal amount, String fromUserId, String toUserId, UUID idempotencyId) {
        return TransactionMessage
                .builder()
                .type(transactionType)
                .userId(userId)
                .transactionId(transactionId)
                .amount(amount)
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .idempotencyId(idempotencyId)
                .build();
    }
}
