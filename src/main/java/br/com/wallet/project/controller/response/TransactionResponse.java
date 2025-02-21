package br.com.wallet.project.controller.response;

import br.com.wallet.project.domain.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema
public class TransactionResponse {

  @Schema(example = "user-id-123", description = "Wallet user id.")
  private String userId;

  @Schema(example = "0000-0000-0000-0000", description = "Transaction identifier.")
  private UUID transactionId;

  @Schema(example = "DEPOSIT", description = "Transaction operation type.")
  private TransactionType transactionType;

  @Schema(example = "10.0", description = "Amount from operation.")
  private BigDecimal amount;
}
