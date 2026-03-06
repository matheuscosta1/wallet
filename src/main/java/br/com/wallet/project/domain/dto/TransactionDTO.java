package br.com.wallet.project.domain.dto;

import br.com.wallet.project.domain.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDTO {

    private Long id;
    private WalletDTO wallet;
    private BigDecimal amount;
    private UUID transactionTrackId;
    private TransactionType type;
    private LocalDateTime timestamp;
    private BigDecimal balanceBeforeTransaction;
    private BigDecimal balanceAfterTransaction;
}
