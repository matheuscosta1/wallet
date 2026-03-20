package br.com.wallet.project.application.strategy;

import br.com.wallet.project.application.command.TransactionCommand;
import br.com.wallet.project.application.port.in.WalletUseCase;
import br.com.wallet.project.application.port.out.TransactionRepository;
import br.com.wallet.project.application.port.out.TransferRepository;
import br.com.wallet.project.application.port.out.WalletEventRepository;
import br.com.wallet.project.domain.model.Transaction;
import br.com.wallet.project.domain.model.Wallet;
import br.com.wallet.project.domain.model.enums.TransactionType;
import br.com.wallet.project.shared.factory.WalletEventFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Slf4j
public class TransferStrategy implements TransactionStrategy {

    private final WalletUseCase walletUseCase;
    private final TransferRepository transferRepository;
    private final WalletEventRepository walletEventRepository;
    private final ObjectProvider<TransactionStrategyFactory> strategyFactoryProvider;

    public TransferStrategy(WalletUseCase walletUseCase,
                            TransferRepository transferRepository,
                            WalletEventRepository walletEventRepository,
                            ObjectProvider<TransactionStrategyFactory> strategyFactoryProvider) {
        this.walletUseCase = walletUseCase;
        this.transferRepository = transferRepository;
        this.walletEventRepository = walletEventRepository;
        this.strategyFactoryProvider = strategyFactoryProvider;
    }

    @Override
    @Transactional("transactionManager")
    public Transaction execute(TransactionCommand command) {
        log.info("Transfer {} from {} to {}",
                command.getTransactionId(),
                command.getFromUserWalletId(),
                command.getToUserWalletId());

        Wallet fromWallet = walletUseCase.validateWallet(
                command.getFromUserWalletId(), command.getTransactionId());
        Wallet toWallet = walletUseCase.validateWallet(
                command.getToUserWalletId(), command.getTransactionId());

        BigDecimal fromBalanceBefore = fromWallet.getBalance();
        BigDecimal toBalanceBefore   = toWallet.getBalance();

        TransactionStrategyFactory strategyFactory = strategyFactoryProvider.getObject();

        TransactionCommand withdrawCmd = TransactionCommand.builder()
                .transactionId(command.getTransactionId())
                .userId(command.getFromUserWalletId())
                .amount(command.getAmount())
                .transactionType(TransactionType.WITHDRAW)
                .build();
        Transaction withdrawTx =
                strategyFactory.getStrategy(TransactionType.WITHDRAW).execute(withdrawCmd);

        TransactionCommand depositCmd = TransactionCommand.builder()
                .transactionId(command.getTransactionId())
                .userId(command.getToUserWalletId())
                .amount(command.getAmount())
                .transactionType(TransactionType.DEPOSIT)
                .build();
        Transaction depositTx =
                strategyFactory.getStrategy(TransactionType.DEPOSIT).execute(depositCmd);

        transferRepository.save(fromWallet, toWallet, withdrawTx, depositTx,
                command.getAmount(), LocalDateTime.now());

        Wallet updatedFrom = walletUseCase.validateWallet(command.getFromUserWalletId());
        Wallet updatedTo   = walletUseCase.validateWallet(command.getToUserWalletId());

        // ── Emit TRANSFER_COMPLETED event ─────────────────────────────────────
        walletEventRepository.save(
                WalletEventFactory.transferCompleted(
                        command,
                        fromBalanceBefore, updatedFrom.getBalance(),
                        toBalanceBefore,   updatedTo.getBalance(),
                        null));

        log.info("Transfer completed {} from {} to {}",
                command.getTransactionId(),
                command.getFromUserWalletId(),
                command.getToUserWalletId());
        return null;
    }
}
