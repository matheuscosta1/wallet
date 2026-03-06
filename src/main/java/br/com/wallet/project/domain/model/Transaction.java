package br.com.wallet.project.domain.model;

import br.com.wallet.project.domain.model.enums.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain model representing a recorded financial transaction.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {

    private Long id;
    private Wallet wallet;
    private BigDecimal amount;
    private UUID transactionTrackId;
    private TransactionType type;
    private LocalDateTime timestamp;
    private BigDecimal balanceBeforeTransaction;
    private BigDecimal balanceAfterTransaction;
}
