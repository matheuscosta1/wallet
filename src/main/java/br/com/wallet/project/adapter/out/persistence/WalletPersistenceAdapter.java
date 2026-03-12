package br.com.wallet.project.adapter.out.persistence;
import br.com.wallet.project.adapter.out.persistence.entity.WalletEntity;
import br.com.wallet.project.adapter.out.persistence.jpa.JpaWalletRepository;
import br.com.wallet.project.application.port.out.WalletRepository;
import br.com.wallet.project.domain.model.Wallet;
import br.com.wallet.project.shared.mapper.WalletEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
/** Driven adapter: implements WalletRepository port using JPA + PostgreSQL. */
@Repository
@RequiredArgsConstructor
public class WalletPersistenceAdapter implements WalletRepository {
    private final JpaWalletRepository jpaWalletRepository;
    @Override
    public Wallet findByUserId(String userId) {
        WalletEntity entity = jpaWalletRepository.findByUserId(userId);
        return WalletEntityMapper.toDomain(entity);
    }
    @Override
    public void save(Wallet wallet) {
        jpaWalletRepository.save(WalletEntityMapper.toEntity(wallet));
    }
}
