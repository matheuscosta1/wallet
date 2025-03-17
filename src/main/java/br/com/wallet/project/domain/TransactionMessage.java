package br.com.wallet.project.domain;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionMessage {
    private UUID transactionId;
    private String idempotencyId;
    private String userId;
    private String fromUserId;
    private String toUserId;
    private BigDecimal amount;
    private TransactionType type;
}

