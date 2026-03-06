package br.com.wallet.project.domain.strategy;

import br.com.wallet.project.domain.TransactionType;
import br.com.wallet.project.domain.dto.TransactionDTO;
import br.com.wallet.project.domain.dto.WalletDTO;
import br.com.wallet.project.domain.request.TransactionRequest;
import br.com.wallet.project.domain.service.WalletValidationService;
import br.com.wallet.project.infrastructure.persistence.TransactionRepository;
import br.com.wallet.project.infrastructure.persistence.WalletRepository;
import br.com.wallet.project.mapper.TransactionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class WithdrawStrategy extends WalletValidationService implements TransactionStrategy {
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public WithdrawStrategy(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        super(walletRepository);
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public TransactionDTO execute(TransactionRequest transactionRequest) {
        log.info("Withdraw funds for user id: {} and transaction id {} started to process", transactionRequest.getUserId(), transactionRequest.getTransactionId());
        WalletDTO wallet = validateWallet(transactionRequest.getUserId(), transactionRequest.getTransactionId());
        BigDecimal actualBalance = wallet.getBalance();
        wallet.withdrawMoney(transactionRequest.getAmount(), transactionRequest.getTransactionId());
        walletRepository.save(wallet);
        TransactionDTO transaction =
            transactionRepository.save(
                TransactionMapper.mapTransactionRequestIntoTransactionEntity(
                        transactionRequest,
                        wallet,
                        TransactionType.WITHDRAW,
                        actualBalance,
                        wallet.getBalance())
            );
        log.info("Successfully withdraw funds for user id: {} and transaction id {}", transactionRequest.getUserId(), transactionRequest.getTransactionId());
        return transaction;
    }
}