package br.com.wallet.project.shared.mapper;
import br.com.wallet.project.adapter.out.persistence.entity.TransactionEntity;
import br.com.wallet.project.domain.model.Transaction;
public class TransactionEntityMapper {
    private TransactionEntityMapper() {}
    public static Transaction toDomain(TransactionEntity entity) {
        if (entity == null) return null;
        return Transaction.builder()
            .id(entity.getId()).wallet(WalletEntityMapper.toDomain(entity.getWalletEntity()))
            .amount(entity.getAmount()).transactionTrackId(entity.getTransactionTrackId())
            .type(entity.getType()).timestamp(entity.getTimestamp())
            .balanceBeforeTransaction(entity.getBalanceBeforeTransaction())
            .balanceAfterTransaction(entity.getBalanceAfterTransaction()).build();
    }
    public static TransactionEntity toEntity(Transaction domain) {
        return TransactionEntity.builder()
            .id(domain.getId()).walletEntity(WalletEntityMapper.toEntity(domain.getWallet()))
            .amount(domain.getAmount()).transactionTrackId(domain.getTransactionTrackId())
            .type(domain.getType()).timestamp(domain.getTimestamp())
            .balanceBeforeTransaction(domain.getBalanceBeforeTransaction())
            .balanceAfterTransaction(domain.getBalanceAfterTransaction()).build();
    }
}
