package br.com.wallet.project.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema
public class WalletResponse {

  @Schema(example = "user-id-123", description = "Wallet user id.")
  private String userId;

  @Schema(example = "0.00", description = "Balance wallet.")
  private BigDecimal balance;
}
