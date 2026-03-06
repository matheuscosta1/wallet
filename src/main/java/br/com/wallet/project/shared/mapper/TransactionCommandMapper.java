package br.com.wallet.project.shared.mapper;
import br.com.wallet.project.adapter.in.web.request.*;
import br.com.wallet.project.application.command.TransactionCommand;
import br.com.wallet.project.domain.model.TransactionMessage;
import br.com.wallet.project.domain.model.enums.TransactionType;
public class TransactionCommandMapper {
    private TransactionCommandMapper() {}
    public static TransactionCommand fromDepositRequest(TransactionOperationRequest r) {
        return TransactionCommand.builder().userId(r.getUserId()).amount(r.getAmount())
            .idempotencyId(r.getIdempotencyId()).transactionType(TransactionType.DEPOSIT).build();
    }
    public static TransactionCommand fromWithdrawRequest(TransactionOperationRequest r) {
        return TransactionCommand.builder().userId(r.getUserId()).amount(r.getAmount())
            .idempotencyId(r.getIdempotencyId()).transactionType(TransactionType.WITHDRAW).build();
    }
    public static TransactionCommand fromTransferRequest(TransferRequest r) {
        return TransactionCommand.builder().fromUserWalletId(r.getFromUserId()).toUserWalletId(r.getToUserId())
            .amount(r.getAmount()).idempotencyId(r.getIdempotencyId()).transactionType(TransactionType.TRANSFER).build();
    }
    public static TransactionCommand fromGenericRequest(GenericTransactionRequest r) {
        return TransactionCommand.builder().userId(r.getUserId()).fromUserWalletId(r.getFromUserId())
            .toUserWalletId(r.getToUserId()).amount(r.getAmount())
            .idempotencyId(r.getIdempotencyId()).transactionType(r.getTransactionType()).build();
    }
    public static TransactionCommand fromMessage(TransactionMessage m) {
        return TransactionCommand.builder().userId(m.getUserId()).fromUserWalletId(m.getFromUserId())
            .toUserWalletId(m.getToUserId()).transactionId(m.getTransactionId())
            .amount(m.getAmount()).transactionType(m.getType()).idempotencyId(m.getIdempotencyId()).build();
    }
}
