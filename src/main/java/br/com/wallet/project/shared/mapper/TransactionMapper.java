package br.com.wallet.project.shared.mapper;
import br.com.wallet.project.application.command.TransactionCommand;
import br.com.wallet.project.domain.model.Transaction;
import br.com.wallet.project.domain.model.Wallet;
import br.com.wallet.project.domain.model.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public class TransactionMapper {
    private TransactionMapper() {}
    public static Transaction fromCommand(TransactionCommand command, Wallet wallet, TransactionType type, BigDecimal balanceBefore, BigDecimal balanceAfter) {
        return Transaction.builder()
            .wallet(wallet).transactionTrackId(command.getTransactionId()).type(type)
            .amount(command.getAmount()).timestamp(LocalDateTime.now())
            .balanceBeforeTransaction(balanceBefore).balanceAfterTransaction(balanceAfter).build();
    }
}
