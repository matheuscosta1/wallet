package br.com.wallet.project.application.command;
import br.com.wallet.project.domain.model.enums.TransactionType;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;
/**
 * Command object representing the intent to execute a financial transaction.
 * Input to the TransactionUseCase driving port.
 */
@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class TransactionCommand {
    private String userId;
    private UUID transactionId;
    private BigDecimal amount;
    private TransactionType transactionType;
    private String fromUserWalletId;
    private String toUserWalletId;
    private UUID idempotencyId;
}
