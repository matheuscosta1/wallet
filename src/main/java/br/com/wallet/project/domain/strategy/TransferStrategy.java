package br.com.wallet.project.domain.strategy;

import br.com.wallet.project.domain.TransactionType;
import br.com.wallet.project.domain.request.TransactionRequest;
import br.com.wallet.project.mapper.TransactionRequestMapper;
import br.com.wallet.project.mapper.TransferMapper;
import br.com.wallet.project.infrastructure.persistence.TransferPersistence;
import br.com.wallet.project.infrastructure.persistence.WalletPersistence;
import br.com.wallet.project.infrastructure.persistence.jpa.repository.JpaTransactionRepository;
import br.com.wallet.project.domain.model.Transaction;
import br.com.wallet.project.domain.model.Wallet;
import br.com.wallet.project.domain.service.WalletValidationService;
import br.com.wallet.project.domain.service.TransactionProcessorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class TransferStrategy extends WalletValidationService implements TransactionStrategy {
    private final TransferPersistence transferPersistence;
    private final TransactionProcessorService transactionProcessorService;

    public TransferStrategy(WalletPersistence walletPersistence,
                            TransferPersistence transferPersistence,
                            JpaTransactionRepository jpaTransactionRepository,
                            TransactionProcessorService transactionProcessorService) {
        super(walletPersistence);
        this.transferPersistence = transferPersistence;
        this.transactionProcessorService = transactionProcessorService;
    }

    @Override
    @Transactional("transactionManager")
    public Transaction execute(TransactionRequest transactionRequest) {
        log.info("Processing transfer for transaction id {}. From user id {} to user id {}",
                transactionRequest.getTransactionId(),
                transactionRequest.getFromUserWalletId(),
                transactionRequest.getToUserWalletId()
        );

        Wallet fromUserWallet = validateWallet(transactionRequest.getFromUserWalletId(), transactionRequest.getTransactionId());
        Wallet toUserWallet = validateWallet(transactionRequest.getToUserWalletId(), transactionRequest.getTransactionId());

        TransactionRequest withdrawRequest = TransactionRequestMapper.mapTransactionRequest(
                transactionRequest.getTransactionId(),
                transactionRequest.getFromUserWalletId(),
                TransactionType.WITHDRAW,
                transactionRequest.getAmount()
        );
        Transaction withdrawTransaction = transactionProcessorService.processTransaction(withdrawRequest, TransactionType.WITHDRAW);

        TransactionRequest depositRequest = TransactionRequestMapper.mapTransactionRequest(
                transactionRequest.getTransactionId(),
                transactionRequest.getToUserWalletId(),
                TransactionType.DEPOSIT,
                transactionRequest.getAmount()
        );
        Transaction depositTransaction = transactionProcessorService.processTransaction(depositRequest, TransactionType.DEPOSIT);

        transferPersistence.save(
                TransferMapper.mapTransferEntity(
                        toUserWallet,
                        fromUserWallet,
                        depositTransaction,
                        withdrawTransaction,
                        transactionRequest.getAmount())
        );

        log.info("Finished transfer for transaction id {}. From user id {} to user id {}",
                transactionRequest.getTransactionId(),
                transactionRequest.getFromUserWalletId(),
                transactionRequest.getToUserWalletId()
        );

        return null;
    }
}
