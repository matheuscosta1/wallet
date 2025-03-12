package br.com.wallet.project.mapper;

import br.com.wallet.project.domain.TransactionType;
import br.com.wallet.project.domain.request.TransactionRequest;
import br.com.wallet.project.domain.model.Transaction;
import br.com.wallet.project.domain.model.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionMapper {
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
