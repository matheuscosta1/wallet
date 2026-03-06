package br.com.wallet.project.shared.mapper;
import br.com.wallet.project.adapter.in.web.response.TransactionHistoryResponse;
import br.com.wallet.project.domain.model.Transaction;
import br.com.wallet.project.shared.util.MoneyUtil;
public class TransactionHistoryMapper {
    private TransactionHistoryMapper() {}
    public static TransactionHistoryResponse toResponse(Transaction transaction) {
        return TransactionHistoryResponse.builder()
            .userId(transaction.getWallet().getUserId()).transactionId(transaction.getTransactionTrackId())
            .transactionType(transaction.getType()).amount(MoneyUtil.format(transaction.getAmount()))
            .date(transaction.getTimestamp())
            .balanceBeforeTransaction(MoneyUtil.format(transaction.getBalanceBeforeTransaction()))
            .balanceAfterTransaction(MoneyUtil.format(transaction.getBalanceAfterTransaction())).build();
    }
}
