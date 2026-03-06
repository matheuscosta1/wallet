package br.com.wallet.project.application.strategy;
import br.com.wallet.project.application.command.TransactionCommand;
import br.com.wallet.project.application.port.in.WalletUseCase;
import br.com.wallet.project.application.port.out.TransferRepository;
import br.com.wallet.project.domain.model.Transaction;
import br.com.wallet.project.domain.model.Wallet;
import br.com.wallet.project.domain.model.enums.TransactionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
@Component
@Slf4j
public class TransferStrategy implements TransactionStrategy {
    private final WalletUseCase walletUseCase;
    private final TransferRepository transferRepository;
    private final ObjectProvider<TransactionStrategyFactory> strategyFactoryProvider;

    public TransferStrategy(WalletUseCase walletUseCase, TransferRepository transferRepository, ObjectProvider<TransactionStrategyFactory> strategyFactoryProvider) {
        this.walletUseCase = walletUseCase;
        this.transferRepository = transferRepository;
        this.strategyFactoryProvider = strategyFactoryProvider;
    }
    @Override
    @Transactional("transactionManager")
    public Transaction execute(TransactionCommand command) {
        log.info("Transfer {} from {} to {}", command.getTransactionId(), command.getFromUserWalletId(), command.getToUserWalletId());
        Wallet fromWallet = walletUseCase.validateWallet(command.getFromUserWalletId(), command.getTransactionId());
        Wallet toWallet = walletUseCase.validateWallet(command.getToUserWalletId(), command.getTransactionId());

        TransactionStrategyFactory strategyFactory = strategyFactoryProvider.getObject();

        TransactionCommand withdrawCmd = TransactionCommand.builder()
            .transactionId(command.getTransactionId()).userId(command.getFromUserWalletId())
            .amount(command.getAmount()).transactionType(TransactionType.WITHDRAW).build();
        Transaction withdrawTx = strategyFactory.getStrategy(TransactionType.WITHDRAW).execute(withdrawCmd);
        TransactionCommand depositCmd = TransactionCommand.builder()
            .transactionId(command.getTransactionId()).userId(command.getToUserWalletId())
            .amount(command.getAmount()).transactionType(TransactionType.DEPOSIT).build();
        Transaction depositTx = strategyFactory.getStrategy(TransactionType.DEPOSIT).execute(depositCmd);
        transferRepository.save(fromWallet, toWallet, withdrawTx, depositTx, command.getAmount(), LocalDateTime.now());
        log.info("Transfer completed {} from {} to {}", command.getTransactionId(), command.getFromUserWalletId(), command.getToUserWalletId());
        return null;
    }
}
