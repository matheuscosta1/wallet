package br.com.wallet.project.domain.strategy;

import br.com.wallet.project.domain.TransactionType;
import br.com.wallet.project.domain.dto.TransactionDTO;
import br.com.wallet.project.domain.dto.WalletDTO;
import br.com.wallet.project.domain.request.TransactionRequest;
import br.com.wallet.project.infrastructure.persistence.TransactionRepository;
import br.com.wallet.project.infrastructure.persistence.WalletRepository;
import br.com.wallet.project.domain.service.WalletValidationService;
import br.com.wallet.project.mapper.TransactionDomainMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class DepositStrategy extends WalletValidationService implements TransactionStrategy {
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public DepositStrategy(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        super(walletRepository);
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public TransactionDTO execute(TransactionRequest transactionRequest) {
        log.info("Deposit funds for user id: {} and transaction id {} started to process",
                transactionRequest.getUserId(), transactionRequest.getTransactionId());
        WalletDTO wallet = validateWallet(transactionRequest.getUserId(), transactionRequest.getTransactionId());
        BigDecimal balanceBefore = wallet.getBalance();
        wallet.depositMoney(transactionRequest.getAmount(), transactionRequest.getTransactionId());
        walletRepository.save(wallet);
        TransactionDTO transaction = transactionRepository.save(
                TransactionDomainMapper.fromTransactionRequest(
                        transactionRequest, wallet, TransactionType.DEPOSIT, balanceBefore, wallet.getBalance()));
        log.info("Finished deposit funds for user id: {} and transaction id {}",
                transactionRequest.getUserId(), transactionRequest.getTransactionId());
        return transaction;
    }
}