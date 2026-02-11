package br.com.wallet.project.controller.request;

import br.com.wallet.project.domain.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema
public class TransactionOperationRequest {

  @Schema(example = "user-account-id", description = "Wallet user identifier.")
  @NotBlank
  private String userId;

  @Schema(example = "10.0", description = "Wallet user identifier.")
  @NotNull
  private BigDecimal amount;

  @Schema(example = "12334002", description = "Unique transaction identifier to avoid duplicates.")
  @NotNull
  private UUID idempotencyId;
}
