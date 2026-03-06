package br.com.wallet.project.mapper;

import br.com.wallet.project.domain.TransactionType;
import br.com.wallet.project.domain.dto.TransactionDTO;
import br.com.wallet.project.domain.dto.WalletDTO;
import br.com.wallet.project.domain.request.TransactionRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionMapper {
    public static TransactionDTO mapTransactionRequestIntoTransactionEntity(TransactionRequest transactionRequest,
                                                                            WalletDTO wallet,
                                                                            TransactionType transactionType,
                                                                            BigDecimal actualBalance,
                                                                            BigDecimal newBalance
    ) {
        return TransactionDTO
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
