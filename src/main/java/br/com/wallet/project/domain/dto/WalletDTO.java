package br.com.wallet.project.domain.dto;

import br.com.wallet.project.domain.enums.WalletErrors;
import br.com.wallet.project.exception.WalletException;
import br.com.wallet.project.util.MoneyUtil;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class WalletDTO {

    private Long id;
    private String userId;
    private BigDecimal balance;
    private Long version;

    public void depositMoney(BigDecimal amount, UUID transactionId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error(WalletErrors.W0007.message(), userId, transactionId);
            throw new WalletException(
                    MessageFormat.format(WalletErrors.W0007.message(), userId, transactionId),
                    WalletErrors.W0007.name(),
                    WalletErrors.W0007.group());
        }
        balance = MoneyUtil.format(balance.add(amount));
    }

    public void withdrawMoney(BigDecimal amount, UUID transactionId) {
        BigDecimal newBalance = MoneyUtil.format(balance.subtract(amount));
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            log.error("Error during withdraw, user id {} has no funds. Transaction id: {}", userId, transactionId);
            throw new WalletException(
                    WalletErrors.W0004.message(),
                    WalletErrors.W0004.name(),
                    WalletErrors.W0004.group());
        }
        balance = newBalance;
    }
}
