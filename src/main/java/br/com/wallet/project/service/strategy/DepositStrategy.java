package br.com.wallet.project.service.strategy;

import br.com.wallet.project.domain.TransactionType;
import br.com.wallet.project.domain.request.TransactionRequest;
import br.com.wallet.project.repositoy.TransactionRepository;
import br.com.wallet.project.repositoy.WalletRepository;
import br.com.wallet.project.repositoy.model.Transaction;
import br.com.wallet.project.repositoy.model.Wallet;
import br.com.wallet.project.service.AbstractWalletService;
import br.com.wallet.project.service.mapper.TransactionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@Slf4j
public class DepositStrategy extends AbstractWalletService implements TransactionStrategy {
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public DepositStrategy(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        super(walletRepository);
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Transaction execute(TransactionRequest transactionRequest) {
        log.info("Deposit funds for user id: {} and transaction id {} started to process", transactionRequest.getUserId(), transactionRequest.getTransactionId());

        Wallet wallet = validateWallet(transactionRequest.getUserId(), transactionRequest.getTransactionId());

        BigDecimal actualBalance = wallet.getBalance().setScale(2, RoundingMode.HALF_DOWN);
        BigDecimal newBalance = actualBalance.add(transactionRequest.getAmount()).setScale(2, RoundingMode.HALF_DOWN);

        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        Transaction transaction = transactionRepository.save(TransactionMapper.mapTransactionRequestIntoTransactionEntity(transactionRequest, wallet, TransactionType.DEPOSIT, actualBalance, newBalance));
        log.info("Finished to deposit funds for user id: {} and transaction id {}", transactionRequest.getUserId(), transactionRequest.getTransactionId());
        return transaction;
    }
}