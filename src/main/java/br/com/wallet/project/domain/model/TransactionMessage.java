package br.com.wallet.project.domain.model;

import br.com.wallet.project.domain.model.enums.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Domain event representing a transaction to be processed asynchronously.
 * Published to the messaging broker (Kafka) by the application layer.
 */
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionMessage {
    private UUID transactionId;
    private UUID idempotencyId;
    private String userId;
    private String fromUserId;
    private String toUserId;
    private BigDecimal amount;
    private TransactionType type;
}

