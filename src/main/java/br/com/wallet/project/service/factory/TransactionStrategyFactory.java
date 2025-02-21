package br.com.wallet.project.service.factory;

import br.com.wallet.project.domain.TransactionType;
import br.com.wallet.project.service.enums.WalletErrors;
import br.com.wallet.project.service.exception.WalletException;
import br.com.wallet.project.service.strategy.DepositStrategy;
import br.com.wallet.project.service.strategy.TransactionStrategy;
import br.com.wallet.project.service.strategy.TransferStrategy;
import br.com.wallet.project.service.strategy.WithdrawStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.Map;

@Component
@Slf4j
public class TransactionStrategyFactory {
    private final Map<TransactionType, TransactionStrategy> strategies;

    public TransactionStrategyFactory(
            DepositStrategy depositStrategy,
            WithdrawStrategy withdrawStrategy,
            TransferStrategy transferStrategy) {
        this.strategies = Map.of(
            TransactionType.DEPOSIT, depositStrategy,
            TransactionType.WITHDRAW, withdrawStrategy,
            TransactionType.TRANSFER, transferStrategy
        );
    }

    public TransactionStrategy getStrategy(TransactionType transactionType) {
        return strategies.getOrDefault(transactionType, request -> {
            log.error("No strategy found for transaction type: {}", transactionType);
            throw new WalletException(
                    MessageFormat.format(
                            WalletErrors.W0005.message(), transactionType),
                    WalletErrors.W0005.name(),
                    WalletErrors.W0005.group());
        });
    }
}