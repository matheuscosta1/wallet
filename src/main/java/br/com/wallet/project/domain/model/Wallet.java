package br.com.wallet.project.domain.model;

import br.com.wallet.project.domain.enums.WalletErrors;
import br.com.wallet.project.exception.WalletException;
import br.com.wallet.project.util.MoneyUtil;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@Table(name = "wallets")
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class Wallet {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userId;

    @Column(nullable = false, scale = 2)
    private BigDecimal balance;

    @Version
    private Long version;
    
    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions;

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
