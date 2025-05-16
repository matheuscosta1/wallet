package br.com.wallet.project.mapper;

import br.com.wallet.project.controller.request.GenericRequest;
import br.com.wallet.project.controller.request.TransactionOperationRequest;
import br.com.wallet.project.controller.request.TransferRequest;
import br.com.wallet.project.domain.TransactionMessage;
import br.com.wallet.project.domain.TransactionType;
import br.com.wallet.project.domain.request.TransactionRequest;
import br.com.wallet.project.domain.request.WalletRequest;

import java.math.BigDecimal;
import java.util.UUID;

public class TransactionRequestMapper {
    public static TransactionRequest mapTransactionMessageToTransactionRequest(TransactionMessage transactionMessage
    ) {
        return TransactionRequest
                .builder()
                .amount(transactionMessage.getAmount())
                .userId(transactionMessage.getUserId())
                .fromUserWalletId(transactionMessage.getFromUserId())
                .toUserWalletId(transactionMessage.getToUserId())
                .transactionId(transactionMessage.getTransactionId())
                .transactionType(transactionMessage.getType())
                .idempotencyId(transactionMessage.getIdempotencyId())
                .build();
    }

    public static TransactionRequest mapTransactionRequest(UUID transactionId, String userId, TransactionType transactionType, BigDecimal amount) {
        return TransactionRequest.builder()
                .transactionId(transactionId)
                .transactionType(transactionType)
                .userId(userId)
                .amount(amount)
                .build();
    }

    public static TransactionRequest mapTransferRequestIntoTransactionRequest(TransferRequest transferRequest) {
        return TransactionRequest.builder()
                .toUserWalletId(transferRequest.getToUserId())
                .fromUserWalletId(transferRequest.getFromUserId())
                .transactionType(TransactionType.TRANSFER)
                .amount(transferRequest.getAmount())
                .userId(null)
                .idempotencyId(transferRequest.getIdempotencyId())
                .build();
    }

    public static TransactionRequest mapTransactionOperationRequestToTransactionRequest(TransactionOperationRequest transactionOperationRequest, TransactionType transactionType) {
        return TransactionRequest.builder()
                .transactionType(transactionType)
                .userId(transactionOperationRequest.getUserId())
                .amount(transactionOperationRequest.getAmount())
                .idempotencyId(transactionOperationRequest.getIdempotencyId())
                .build();
    }

    public static TransactionRequest mapGenericRequestToTransactionRequest(GenericRequest genericRequest) {
        return TransactionRequest.builder()
                .toUserWalletId(genericRequest.getToUserId())
                .fromUserWalletId(genericRequest.getFromUserId())
                .transactionType(genericRequest.getTransactionType())
                .amount(genericRequest.getAmount())
                .userId(genericRequest.getUserId())
                .idempotencyId(genericRequest.getIdempotencyId())
                .build();
    }

}
