package br.com.wallet.project.domain.service;

import br.com.wallet.project.controller.request.TransferRequest;
import br.com.wallet.project.controller.response.TransactionResponse;
import br.com.wallet.project.domain.TransactionMessage;
import br.com.wallet.project.domain.TransactionType;
import br.com.wallet.project.domain.request.TransactionRequest;
import br.com.wallet.project.infrastructure.messaging.WalletTransactionProducer;
import br.com.wallet.project.infrastructure.persistence.WalletPersistence;
import br.com.wallet.project.mapper.TransactionMessageMapper;
import br.com.wallet.project.mapper.TransactionResponseMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
public class WalletTransactionService extends WalletValidationService implements WalletTransaction {
    private final WalletTransactionProducer walletTransactionProducer;

    public WalletTransactionService(WalletTransactionProducer walletTransactionProducer, WalletPersistence walletPersistence) {
        super(walletPersistence);
        this.walletTransactionProducer = walletTransactionProducer;
    }


    @Override
    @Transactional("transactionManager")
    public TransactionResponse processDeposit(TransactionRequest transactionOperationRequest, UUID transactionId) {
        return transactionProcessor(transactionOperationRequest, transactionId);
    }

    @Override
    @Transactional("transactionManager")
    public TransactionResponse processWithdraw(TransactionRequest transactionOperationRequest, UUID transactionId) {
        return transactionProcessor(transactionOperationRequest, transactionId);
    }

    @Override
    @Transactional("transactionManager")
    public TransactionResponse processTransfer(TransactionRequest transactionOperationRequest, UUID transactionId) {
        log.info("Transfer funds from user id {} to user id {} with transaction id {}",
                transactionOperationRequest.getFromUserWalletId(), transactionOperationRequest.getToUserWalletId(), transactionId);

        validateWallet(transactionOperationRequest.getFromUserWalletId(), transactionId);
        validateWallet(transactionOperationRequest.getToUserWalletId(), transactionId);

        TransactionMessage transactionMessage =
                TransactionMessageMapper.mapTransactionMessage(
                        null,
                        transactionId,
                        TransactionType.TRANSFER,
                        transactionOperationRequest.getAmount(),
                        transactionOperationRequest.getFromUserWalletId(),
                        transactionOperationRequest.getToUserWalletId()
                );
        walletTransactionProducer.sendMessage(transactionMessage);
        log.info("Transfer message sent for user id {} to user id {} with transaction id {}",
                transactionOperationRequest.getFromUserWalletId(), transactionOperationRequest.getToUserWalletId(), transactionId);
        return TransactionResponseMapper.mapTransactionResponse(transactionMessage, TransactionType.TRANSFER);
    }

    private TransactionResponse transactionProcessor(TransactionRequest transactionOperationRequest, UUID transactionId) {
        log.info("{} funds for user id {} and transaction id {}", transactionOperationRequest.getTransactionType(), transactionOperationRequest.getUserId(), transactionId);
        validateWallet(transactionOperationRequest.getUserId(), transactionId);
        TransactionMessage transactionMessage =
                TransactionMessageMapper.mapTransactionMessage(
                        transactionOperationRequest.getUserId(),
                        transactionId,
                        transactionOperationRequest.getTransactionType(),
                        transactionOperationRequest.getAmount(),
                        null,
                        null
                );
        walletTransactionProducer.sendMessage(transactionMessage);
        log.info("{} message sent for user id {} and transaction id {}", transactionOperationRequest.getTransactionType(), transactionOperationRequest.getUserId(), transactionId);
        return TransactionResponseMapper.mapTransactionResponse(transactionMessage, transactionOperationRequest.getTransactionType());
    }

}