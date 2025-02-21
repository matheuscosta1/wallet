package br.com.wallet.project.service.strategy;

import br.com.wallet.project.domain.TransactionType;
import br.com.wallet.project.domain.request.TransactionRequest;
import br.com.wallet.project.repositoy.TransactionRepository;
import br.com.wallet.project.repositoy.WalletRepository;
import br.com.wallet.project.repositoy.model.Transaction;
import br.com.wallet.project.repositoy.model.Wallet;
import br.com.wallet.project.service.AbstractWalletService;
import br.com.wallet.project.service.enums.WalletErrors;
import br.com.wallet.project.service.exception.WalletException;
import br.com.wallet.project.service.mapper.TransactionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.MessageFormat;

@Component
@Slf4j
public class WithdrawStrategy extends AbstractWalletService implements TransactionStrategy {
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public WithdrawStrategy(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        super(walletRepository);
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
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
        walletRepository.save(wallet);

        Transaction transaction = transactionRepository.save(TransactionMapper.mapTransactionRequestIntoTransactionEntity(transactionRequest, wallet, TransactionType.WITHDRAW, actualBalance, newBalance));
        log.info("Successfully withdraw funds for user id: {} and transaction id {}", transactionRequest.getUserId(), transactionRequest.getTransactionId());
        return transaction;
    }
}