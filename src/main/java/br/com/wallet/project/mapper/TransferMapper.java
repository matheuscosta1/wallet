package br.com.wallet.project.mapper;

import br.com.wallet.project.domain.TransactionType;
import br.com.wallet.project.domain.model.Transfer;
import br.com.wallet.project.domain.request.TransactionRequest;
import br.com.wallet.project.domain.model.Transaction;
import br.com.wallet.project.domain.model.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransferMapper {
    public static Transfer mapTransferEntity(Wallet toUserWallet, Wallet fromUserWallet, Transaction depositTransaction, Transaction withdrawTransaction, BigDecimal amount) {
        return Transfer
                .builder()
                .timestamp(LocalDateTime.now())
                .toWallet(toUserWallet)
                .fromWallet(fromUserWallet)
                .amount(amount)
                .debitTransaction(withdrawTransaction)
                .creditTransaction(depositTransaction)
                .build();
    }
}
