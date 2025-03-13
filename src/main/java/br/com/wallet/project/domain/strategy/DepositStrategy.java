package br.com.wallet.project.domain.strategy;

import br.com.wallet.project.domain.TransactionType;
import br.com.wallet.project.domain.request.TransactionRequest;
import br.com.wallet.project.infrastructure.persistence.TransactionPersistence;
import br.com.wallet.project.infrastructure.persistence.WalletPersistence;
import br.com.wallet.project.domain.model.Transaction;
import br.com.wallet.project.domain.model.Wallet;
import br.com.wallet.project.domain.service.WalletValidationService;
import br.com.wallet.project.mapper.TransactionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@Slf4j
public class DepositStrategy extends WalletValidationService implements TransactionStrategy {
    private final WalletPersistence walletPersistence;
    private final TransactionPersistence transactionPersistence;

    public DepositStrategy(WalletPersistence walletPersistence, TransactionPersistence transactionPersistence) {
        super(walletPersistence);
        this.walletPersistence = walletPersistence;
        this.transactionPersistence = transactionPersistence;
    }

    @Override
    public Transaction execute(TransactionRequest transactionRequest) {
        log.info("Deposit funds for user id: {} and transaction id {} started to process", transactionRequest.getUserId(), transactionRequest.getTransactionId());
        Wallet wallet = validateWallet(transactionRequest.getUserId(), transactionRequest.getTransactionId());

        BigDecimal actualBalance = wallet.getBalance().setScale(2, RoundingMode.HALF_DOWN);
        BigDecimal newBalance = actualBalance.add(transactionRequest.getAmount()).setScale(2, RoundingMode.HALF_DOWN);

        wallet.setBalance(newBalance);
        walletPersistence.save(wallet);

        Transaction transaction = transactionPersistence.save(TransactionMapper.mapTransactionRequestIntoTransactionEntity(transactionRequest, wallet, TransactionType.DEPOSIT, actualBalance, newBalance));
        log.info("Finished to deposit funds for user id: {} and transaction id {}", transactionRequest.getUserId(), transactionRequest.getTransactionId());
        return transaction;
    }
}