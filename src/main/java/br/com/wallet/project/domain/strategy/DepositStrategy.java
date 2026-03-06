package br.com.wallet.project.domain.strategy;

import br.com.wallet.project.domain.TransactionType;
import br.com.wallet.project.domain.dto.TransactionDTO;
import br.com.wallet.project.domain.dto.WalletDTO;
import br.com.wallet.project.domain.request.TransactionRequest;
import br.com.wallet.project.infrastructure.persistence.TransactionPersistence;
import br.com.wallet.project.infrastructure.persistence.WalletPersistence;
import br.com.wallet.project.domain.service.WalletValidationService;
import br.com.wallet.project.mapper.TransactionDomainMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

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
    public TransactionDTO execute(TransactionRequest transactionRequest) {
        log.info("Deposit funds for user id: {} and transaction id {} started to process",
                transactionRequest.getUserId(), transactionRequest.getTransactionId());
        WalletDTO wallet = validateWallet(transactionRequest.getUserId(), transactionRequest.getTransactionId());
        BigDecimal balanceBefore = wallet.getBalance();
        wallet.depositMoney(transactionRequest.getAmount(), transactionRequest.getTransactionId());
        walletPersistence.save(wallet);
        TransactionDTO transaction = transactionPersistence.save(
                TransactionDomainMapper.fromTransactionRequest(
                        transactionRequest, wallet, TransactionType.DEPOSIT, balanceBefore, wallet.getBalance()));
        log.info("Finished deposit funds for user id: {} and transaction id {}",
                transactionRequest.getUserId(), transactionRequest.getTransactionId());
        return transaction;
    }
}