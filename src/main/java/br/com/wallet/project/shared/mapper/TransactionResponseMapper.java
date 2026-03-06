package br.com.wallet.project.shared.mapper;
import br.com.wallet.project.adapter.in.web.response.TransactionResponse;
import br.com.wallet.project.domain.model.TransactionMessage;
import br.com.wallet.project.domain.model.enums.TransactionType;
import br.com.wallet.project.shared.util.MoneyUtil;
public class TransactionResponseMapper {
    private TransactionResponseMapper() {}
    public static TransactionResponse toResponse(TransactionMessage message, TransactionType type) {
        return TransactionResponse.builder()
            .transactionId(message.getTransactionId()).userId(message.getUserId())
            .amount(MoneyUtil.format(message.getAmount())).transactionType(type).build();
    }
}
