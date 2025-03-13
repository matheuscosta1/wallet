package br.com.wallet.project.mapper;

import br.com.wallet.project.controller.response.TransactionResponse;
import br.com.wallet.project.domain.TransactionMessage;
import br.com.wallet.project.domain.TransactionType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public class TransactionResponseMapper {
    public static TransactionResponse mapTransactionResponse(TransactionMessage transactionMessage, TransactionType transactionType) {
        return TransactionResponse
                .builder()
                .transactionId(transactionMessage.getTransactionId())
                .userId(transactionMessage.getUserId())
                .amount(transactionMessage.getAmount().setScale(2,  RoundingMode.DOWN))
                .transactionType(transactionType)
                .build();
    }
}
