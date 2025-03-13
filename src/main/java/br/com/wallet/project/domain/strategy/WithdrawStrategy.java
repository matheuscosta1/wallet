package br.com.wallet.project.domain.strategy;

import br.com.wallet.project.domain.TransactionType;
import br.com.wallet.project.domain.request.TransactionRequest;
import br.com.wallet.project.infrastructure.persistence.TransactionPersistence;
import br.com.wallet.project.infrastructure.persistence.WalletPersistence;
import br.com.wallet.project.domain.model.Transaction;
import br.com.wallet.project.domain.model.Wallet;
import br.com.wallet.project.domain.service.WalletValidationService;
import br.com.wallet.project.domain.enums.WalletErrors;
import br.com.wallet.project.exception.WalletException;
import br.com.wallet.project.mapper.TransactionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.MessageFormat;

@Component
@Slf4j
public class WithdrawStrategy extends WalletValidationService implements TransactionStrategy {
    private final WalletPersistence walletPersistence;
    private final TransactionPersistence transactionPersistence;

    public WithdrawStrategy(WalletPersistence walletPersistence, TransactionPersistence transactionPersistence) {
        super(walletPersistence);
        this.walletPersistence = walletPersistence;
        this.transactionPersistence = transactionPersistence;
    }

    @Override
    public Transaction execute(TransactionRequest transactionRequest) {
        log.info("Withdraw funds for user id: {} and transaction id {} started to process", transactionRequest.getUserId(), transactionRequest.getTransactionId());

        Wallet wallet = validateWallet(transactionRequest.getUserId(), transactionRequest.getTransactionId());

        BigDecimal actualBalance = wallet.getBalance();
        BigDecimal newBalance = actualBalance.subtract(transactionRequest.getAmount());

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            log.error("Error during withdraw, user id {} has no funds. Transaction id: {}", transactionRequest.getUserId(), transactionRequest.getTransactionId());
            throw new WalletException(
                    MessageFormat.format(
                            WalletErrors.W0004.message(), transactionRequest.getTransactionType()),
                    WalletErrors.W0004.name(),
                    WalletErrors.W0004.group());
        }

        wallet.setBalance(newBalance);
        walletPersistence.save(wallet);

        Transaction transaction = transactionPersistence.save(TransactionMapper.mapTransactionRequestIntoTransactionEntity(transactionRequest, wallet, TransactionType.WITHDRAW, actualBalance, newBalance));
        log.info("Successfully withdraw funds for user id: {} and transaction id {}", transactionRequest.getUserId(), transactionRequest.getTransactionId());
        return transaction;
    }
}