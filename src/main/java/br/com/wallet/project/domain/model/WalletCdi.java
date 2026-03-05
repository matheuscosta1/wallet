package br.com.wallet.project.domain.model;

import br.com.wallet.project.domain.enums.WalletErrors;
import br.com.wallet.project.exception.WalletException;
import br.com.wallet.project.util.MoneyUtil;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class WalletCdi {

    private static final BigDecimal CDI_FACTOR = new BigDecimal("0.1");
   private String userId;
   private BigDecimal balance;
   private BigDecimal cdiValue;
   private LocalDateTime createdAt;



    public void calculateCdi() {
        if (balance == null) this.cdiValue=BigDecimal.ZERO;
        this.cdiValue = balance.multiply(CDI_FACTOR);
    }

}
