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
public class Transactions {


  private UUID transactionId;
  private TransactionType transactionType;
}
