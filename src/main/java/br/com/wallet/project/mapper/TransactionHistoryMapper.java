package br.com.wallet.project.mapper;

import br.com.wallet.project.controller.response.TransactionHistoryResponse;
import br.com.wallet.project.domain.TransactionType;
import br.com.wallet.project.domain.model.Transaction;
import br.com.wallet.project.domain.model.Wallet;
import br.com.wallet.project.domain.request.TransactionRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class TransactionHistoryMapper {
    public static TransactionHistoryResponse mapToTransactionHistoryResponse(Transaction transaction) {
        return TransactionHistoryResponse.builder()
                .userId(transaction.getWallet().getUserId())
                .transactionId(transaction.getTransactionTrackId())
                .transactionType(transaction.getType())
                .amount(transaction.getAmount().setScale(2,  RoundingMode.DOWN))
                .date(transaction.getTimestamp())
                .balanceBeforeTransaction(transaction.getBalanceBeforeTransaction().setScale(2,  RoundingMode.DOWN))
                .balanceAfterTransaction(transaction.getBalanceAfterTransaction().setScale(2,  RoundingMode.DOWN))
                .build();
    }
}
