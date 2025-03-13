package br.com.wallet.project.mapper;

import br.com.wallet.project.controller.response.TransactionHistoryResponse;
import br.com.wallet.project.domain.TransactionType;
import br.com.wallet.project.domain.model.Transaction;
import br.com.wallet.project.domain.model.Wallet;
import br.com.wallet.project.domain.request.TransactionRequest;
import br.com.wallet.project.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class TransactionHistoryMapper {
    public static TransactionHistoryResponse mapToTransactionHistoryResponse(Transaction transaction) {
        return TransactionHistoryResponse.builder()
                .userId(transaction.getWallet().getUserId())
                .transactionId(transaction.getTransactionTrackId())
                .transactionType(transaction.getType())
                .amount(MoneyUtil.format(transaction.getAmount()))
                .date(transaction.getTimestamp())
                .balanceBeforeTransaction(MoneyUtil.format(transaction.getBalanceBeforeTransaction()))
                .balanceAfterTransaction(MoneyUtil.format(transaction.getBalanceAfterTransaction()))
                .build();
    }
}
