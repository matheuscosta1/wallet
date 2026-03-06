package br.com.wallet.project.shared.mapper;
import br.com.wallet.project.application.command.TransactionCommand;
import br.com.wallet.project.domain.model.TransactionMessage;
import java.util.UUID;
public class TransactionMessageMapper {
    private TransactionMessageMapper() {}
    public static TransactionMessage toMessage(TransactionCommand command, UUID idempotencyId) {
        return TransactionMessage.builder()
            .transactionId(UUID.randomUUID()).idempotencyId(idempotencyId)
            .userId(command.getUserId()).fromUserId(command.getFromUserWalletId())
            .toUserId(command.getToUserWalletId()).amount(command.getAmount()).type(command.getTransactionType()).build();
    }
}
