package br.com.wallet.project.service.strategy;

import br.com.wallet.project.domain.TransactionType;
import br.com.wallet.project.domain.request.TransactionRequest;
import br.com.wallet.project.repositoy.TransactionRepository;
import br.com.wallet.project.repositoy.TransferRepository;
import br.com.wallet.project.repositoy.WalletRepository;
import br.com.wallet.project.repositoy.model.Transaction;
import br.com.wallet.project.repositoy.model.Transfer;
import br.com.wallet.project.repositoy.model.Wallet;
import br.com.wallet.project.service.AbstractWalletService;
import br.com.wallet.project.service.TransactionProcessorService;
import br.com.wallet.project.service.enums.WalletErrors;
import br.com.wallet.project.service.exception.WalletException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
public class TransferStrategy extends AbstractWalletService implements TransactionStrategy {
    private final WalletRepository walletRepository;
    private final TransferRepository transferRepository;
    private final TransactionProcessorService transactionProcessorService;

    public TransferStrategy(WalletRepository walletRepository,
                            TransferRepository transferRepository,
                            TransactionRepository transactionRepository,
                            TransactionProcessorService transactionProcessorService) {
        super(walletRepository);
        this.walletRepository = walletRepository;
        this.transferRepository = transferRepository;
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

        TransactionRequest withdrawRequest = buildTransactionRequest(
                transactionRequest.getTransactionId(),
                transactionRequest.getFromUserWalletId(),
                TransactionType.WITHDRAW,
                transactionRequest.getAmount()
        );
        Transaction withdrawTransaction = transactionProcessorService.processTransaction(withdrawRequest, TransactionType.WITHDRAW);

        TransactionRequest depositRequest = buildTransactionRequest(
                transactionRequest.getTransactionId(),
                transactionRequest.getToUserWalletId(),
                TransactionType.DEPOSIT,
                transactionRequest.getAmount()
        );
        Transaction depositTransaction = transactionProcessorService.processTransaction(depositRequest, TransactionType.DEPOSIT);

        transferRepository.save(
                buildTransferEntity(
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

    private TransactionRequest buildTransactionRequest(UUID transactionId, String userId, TransactionType transactionType, BigDecimal amount) {
        return TransactionRequest.builder()
                .transactionId(transactionId)
                .transactionType(transactionType)
                .userId(userId)
                .amount(amount)
                .build();
    }

    private Transfer buildTransferEntity(Wallet toUserWallet, Wallet fromUserWallet, Transaction depositTransaction, Transaction withdrawTransaction, BigDecimal amount) {
        return Transfer
                .builder()
                .timestamp(LocalDateTime.now())
                .toWallet(toUserWallet)
                .fromWallet(fromUserWallet)
                .amount(amount)
                .debitTransaction(withdrawTransaction)
                .creditTransaction(depositTransaction)
                .build();
    }
}
