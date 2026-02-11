package br.com.wallet.project.controller.request;

import br.com.wallet.project.controller.validation.DepositValidationGroup;
import br.com.wallet.project.controller.validation.TransferValidationGroup;
import br.com.wallet.project.controller.validation.ValidTransactionRequest;
import br.com.wallet.project.controller.validation.WithdrawValidationGroup;
import br.com.wallet.project.domain.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema
@ValidTransactionRequest
public class GenericRequest {

  @Schema(example = "user-account-id", description = "Wallet user identifier.")
  @NotBlank(groups = {DepositValidationGroup.class, WithdrawValidationGroup.class})
  private String userId;

  @Schema(example = "user-account-id-0", description = "Wallet user id that will transfer the money.")
  @NotBlank(groups = {TransferValidationGroup.class})
  private String fromUserId;

  @Schema(example = "user-account-id-1", description = "Wallet user id that will receive the money.")
  @NotBlank(groups = {TransferValidationGroup.class})
  private String toUserId;

  @Schema(example = "10.0", description = "Transfer amount.")
  @NotNull
  private BigDecimal amount;

  @Schema(example = "12334002", description = "Unique transaction identifier to avoid duplicates.")
  @NotBlank
  private String idempotencyId;

  @Schema(example = "TRANSFER", description = "Transaction type.")
  @NotNull
  private TransactionType transactionType;
}
