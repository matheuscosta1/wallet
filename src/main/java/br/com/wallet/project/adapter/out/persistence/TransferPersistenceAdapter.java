package br.com.wallet.project.adapter.out.persistence;
import br.com.wallet.project.adapter.out.persistence.entity.TransferEntity;
import br.com.wallet.project.adapter.out.persistence.jpa.JpaTransferRepository;
import br.com.wallet.project.application.port.out.TransferRepository;
import br.com.wallet.project.domain.model.Transaction;
import br.com.wallet.project.domain.model.Wallet;
import br.com.wallet.project.shared.mapper.TransactionEntityMapper;
import br.com.wallet.project.shared.mapper.WalletEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/** Driven adapter: implements TransferRepository port using JPA + PostgreSQL. */
@Repository
@RequiredArgsConstructor
public class TransferPersistenceAdapter implements TransferRepository {
    private final JpaTransferRepository jpaTransferRepository;
    @Override
    public void save(Wallet fromWallet, Wallet toWallet, Transaction debitTransaction, Transaction creditTransaction, BigDecimal amount, LocalDateTime timestamp) {
        jpaTransferRepository.save(TransferEntity.builder()
            .fromWalletEntity(WalletEntityMapper.toEntity(fromWallet))
            .toWalletEntity(WalletEntityMapper.toEntity(toWallet))
            .debitTransactionEntity(TransactionEntityMapper.toEntity(debitTransaction))
            .creditTransactionEntity(TransactionEntityMapper.toEntity(creditTransaction))
            .amount(amount).timestamp(timestamp).build());
    }
}
