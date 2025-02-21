package br.com.wallet.project.service.mapper;

import br.com.wallet.project.domain.TransactionType;
import br.com.wallet.project.domain.request.TransactionRequest;
import br.com.wallet.project.repositoy.model.Transaction;
import br.com.wallet.project.repositoy.model.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransferMapper {
    public static Transaction mapTransactionRequestIntoTransactionEntity(TransactionRequest transactionRequest,
                                               Wallet wallet,
                                               TransactionType transactionType,
                                               BigDecimal actualBalance,
                                               BigDecimal newBalance
    ) {
        return Transaction
                .builder()
                .transactionTrackId(transactionRequest.getTransactionId())
                .type(transactionType)
                .balanceBeforeTransaction(actualBalance)
                .balanceAfterTransaction(newBalance)
                .amount(transactionRequest.getAmount())
                .wallet(wallet)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
