package br.com.wallet.project.mapper;

import br.com.wallet.project.domain.TransactionType;
import br.com.wallet.project.domain.dto.TransactionDTO;
import br.com.wallet.project.domain.dto.WalletDTO;
import br.com.wallet.project.domain.model.TransactionEntity;
import br.com.wallet.project.domain.request.TransactionRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionDomainMapper {

    private TransactionDomainMapper() {}

    public static TransactionDTO toTransactionDomain(TransactionEntity transactionEntity) {
        if (transactionEntity == null) return null;
        return TransactionDTO.builder()
                .id(transactionEntity.getId())
                .wallet(WalletDomainMapper.toWalletDomain(transactionEntity.getWalletEntity()))
                .amount(transactionEntity.getAmount())
                .transactionTrackId(transactionEntity.getTransactionTrackId())
                .type(transactionEntity.getType())
                .timestamp(transactionEntity.getTimestamp())
                .balanceBeforeTransaction(transactionEntity.getBalanceBeforeTransaction())
                .balanceAfterTransaction(transactionEntity.getBalanceAfterTransaction())
                .build();
    }

    public static TransactionEntity toTransactionEntity(TransactionDTO domain) {
        return TransactionEntity.builder()
                .id(domain.getId())
                .walletEntity(WalletDomainMapper.toWalletEntity(domain.getWallet()))
                .amount(domain.getAmount())
                .transactionTrackId(domain.getTransactionTrackId())
                .type(domain.getType())
                .timestamp(domain.getTimestamp())
                .balanceBeforeTransaction(domain.getBalanceBeforeTransaction())
                .balanceAfterTransaction(domain.getBalanceAfterTransaction())
                .build();
    }

    public static TransactionDTO fromTransactionRequest(TransactionRequest request,
                                                        WalletDTO wallet,
                                                        TransactionType type,
                                                        BigDecimal balanceBefore,
                                                        BigDecimal balanceAfter) {
        return TransactionDTO.builder()
                .wallet(wallet)
                .transactionTrackId(request.getTransactionId())
                .type(type)
                .amount(request.getAmount())
                .timestamp(LocalDateTime.now())
                .balanceBeforeTransaction(balanceBefore)
                .balanceAfterTransaction(balanceAfter)
                .build();
    }
}
