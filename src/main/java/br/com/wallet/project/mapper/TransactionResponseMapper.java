package br.com.wallet.project.mapper;

import br.com.wallet.project.controller.response.TransactionResponse;
import br.com.wallet.project.domain.TransactionMessage;
import br.com.wallet.project.domain.TransactionType;
import br.com.wallet.project.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public class TransactionResponseMapper {
    public static TransactionResponse mapTransactionResponse(TransactionMessage transactionMessage, TransactionType transactionType) {
        return TransactionResponse
                .builder()
                .transactionId(transactionMessage.getTransactionId())
                .userId(transactionMessage.getUserId())
                .amount(MoneyUtil.format(transactionMessage.getAmount()))
                .transactionType(transactionType)
                .build();
    }
}
