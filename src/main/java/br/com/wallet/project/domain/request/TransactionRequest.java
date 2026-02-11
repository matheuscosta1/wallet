package br.com.wallet.project.domain.request;

import br.com.wallet.project.domain.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionRequest {

  private String userId;
  private UUID transactionId;
  private BigDecimal amount;
  private TransactionType transactionType;
  private String fromUserWalletId;
  private String toUserWalletId;
  private UUID idempotencyId;
}
