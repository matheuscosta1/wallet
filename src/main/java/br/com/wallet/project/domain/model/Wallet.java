package br.com.wallet.project.domain.model;

import br.com.wallet.project.domain.exception.WalletDomainException;
import br.com.wallet.project.domain.exception.WalletErrors;
import br.com.wallet.project.shared.util.MoneyUtil;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.UUID;

/**
 * Aggregate root representing a user's wallet.
 * Contains the core business rules for deposit and withdraw operations.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class Wallet {

    private Long id;
    private String userId;
    private BigDecimal balance;
    private Long version;

    public void deposit(BigDecimal amount, UUID transactionId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error(WalletErrors.W0007.message(), userId, transactionId);
            throw new WalletDomainException(
                    MessageFormat.format(WalletErrors.W0007.message(), userId, transactionId),
                    WalletErrors.W0007.name(),
                    WalletErrors.W0007.group());
        }
        balance = MoneyUtil.format(balance.add(amount));
    }

    public void withdraw(BigDecimal amount, UUID transactionId) {
        BigDecimal newBalance = MoneyUtil.format(balance.subtract(amount));
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            log.error("Insufficient funds for user id {}. Transaction id: {}", userId, transactionId);
            throw new WalletDomainException(
                    WalletErrors.W0004.message(),
                    WalletErrors.W0004.name(),
                    WalletErrors.W0004.group());
        }
        balance = newBalance;
    }
}
