package br.com.wallet.project.domain.service;

import br.com.wallet.project.controller.response.TransactionResponse;
import br.com.wallet.project.domain.TransactionMessage;
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
public class WalletTransactionProcessorServiceMessage extends WalletValidationService implements WalletTransactionMessage {
    private final WalletTransactionProducer walletTransactionProducer;

    public WalletTransactionProcessorServiceMessage(WalletTransactionProducer walletTransactionProducer, WalletPersistence walletPersistence) {
        super(walletPersistence);
        this.walletTransactionProducer = walletTransactionProducer;
    }

    @Override
    @Transactional("transactionManager")
    public TransactionResponse processTransactionMessage(TransactionRequest transactionOperationRequest, UUID transactionId) {
        log.info("{} funds for transaction id {}", transactionOperationRequest.getTransactionType(), transactionId);
        TransactionMessage transactionMessage =
                TransactionMessageMapper.mapTransactionMessage(
                        transactionOperationRequest.getUserId(),
                        transactionId,
                        transactionOperationRequest.getTransactionType(),
                        transactionOperationRequest.getAmount(),
                        transactionOperationRequest.getFromUserWalletId(),
                        transactionOperationRequest.getToUserWalletId()
                );
        walletTransactionProducer.sendMessage(transactionMessage);
        log.info("{} message sent for transaction id {}", transactionOperationRequest.getTransactionType(), transactionId);
        return TransactionResponseMapper.mapTransactionResponse(transactionMessage, transactionOperationRequest.getTransactionType());
    }
}