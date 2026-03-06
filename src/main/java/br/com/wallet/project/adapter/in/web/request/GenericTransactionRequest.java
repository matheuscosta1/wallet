package br.com.wallet.project.adapter.in.web.request;
import br.com.wallet.project.adapter.in.web.validation.ValidGenericTransaction;
import br.com.wallet.project.adapter.in.web.validation.group.DepositValidationGroup;
import br.com.wallet.project.adapter.in.web.validation.group.TransferValidationGroup;
import br.com.wallet.project.adapter.in.web.validation.group.WithdrawValidationGroup;
import br.com.wallet.project.domain.model.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;
@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder @Schema
@ValidGenericTransaction
public class GenericTransactionRequest {
    @Schema(example = "user-account-id", description = "Wallet user identifier (deposit/withdraw).")
    @NotBlank(groups = {DepositValidationGroup.class, WithdrawValidationGroup.class})
    private String userId;
    @Schema(example = "user-id-from", description = "Sender user id (transfer).")
    @NotBlank(groups = {TransferValidationGroup.class})
    private String fromUserId;
    @Schema(example = "user-id-to", description = "Receiver user id (transfer).")
    @NotBlank(groups = {TransferValidationGroup.class})
    private String toUserId;
    @Schema(example = "10.0", description = "Transaction amount.")
    @NotNull
    private BigDecimal amount;
    @Schema(example = "550e8400-e29b-41d4-a716-446655440000", description = "Unique idempotency key.")
    @NotNull
    private UUID idempotencyId;
    @Schema(example = "TRANSFER", description = "Transaction type.")
    @NotNull
    private TransactionType transactionType;
}
