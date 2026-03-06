package br.com.wallet.project.mapper;

import br.com.wallet.project.domain.dto.TransactionDTO;
import br.com.wallet.project.domain.dto.WalletDTO;
import br.com.wallet.project.domain.model.TransferEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransferMapper {
    public static TransferEntity mapTransferEntity(WalletDTO toUserWallet, WalletDTO fromUserWallet, TransactionDTO depositTransaction, TransactionDTO withdrawTransaction, BigDecimal amount) {
        return TransferEntity
                .builder()
                .timestamp(LocalDateTime.now())
                .toWalletEntity(WalletDomainMapper.toWalletEntity(toUserWallet))
                .fromWalletEntity(WalletDomainMapper.toWalletEntity(fromUserWallet))
                .amount(amount)
                .debitTransactionEntity(TransactionDomainMapper.toTransactionEntity(withdrawTransaction))
                .creditTransactionEntity(TransactionDomainMapper.toTransactionEntity(depositTransaction))
                .build();
    }
}
