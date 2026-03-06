package br.com.wallet.project.application.strategy;
import br.com.wallet.project.domain.exception.WalletDomainException;
import br.com.wallet.project.domain.exception.WalletErrors;
import br.com.wallet.project.domain.model.enums.TransactionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.text.MessageFormat;
import java.util.Map;
@Component
@Slf4j
public class TransactionStrategyFactory {
    private final Map<TransactionType, TransactionStrategy> strategies;
    public TransactionStrategyFactory(DepositStrategy depositStrategy, WithdrawStrategy withdrawStrategy, TransferStrategy transferStrategy) {
        this.strategies = Map.of(
            TransactionType.DEPOSIT, depositStrategy,
            TransactionType.WITHDRAW, withdrawStrategy,
            TransactionType.TRANSFER, transferStrategy);
    }
    public TransactionStrategy getStrategy(TransactionType type) {
        return strategies.getOrDefault(type, command -> {
            log.error("No strategy found for transaction type: {}", type);
            throw new WalletDomainException(
                MessageFormat.format(WalletErrors.W0005.message(), type),
                WalletErrors.W0005.name(), WalletErrors.W0005.group());
        });
    }
}
