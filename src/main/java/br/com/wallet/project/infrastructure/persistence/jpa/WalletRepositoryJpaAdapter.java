package br.com.wallet.project.infrastructure.persistence.jpa;

import br.com.wallet.project.domain.dto.WalletDTO;
import br.com.wallet.project.domain.model.WalletEntity;
import br.com.wallet.project.infrastructure.persistence.WalletRepository;
import br.com.wallet.project.infrastructure.persistence.jpa.repository.JpaWalletRepository;
import br.com.wallet.project.mapper.WalletDomainMapper;
import org.springframework.stereotype.Repository;

@Repository
public class WalletRepositoryJpaAdapter implements WalletRepository {
    private final JpaWalletRepository jpaWalletRepository;

    public WalletRepositoryJpaAdapter(JpaWalletRepository jpaWalletRepository) {
        this.jpaWalletRepository = jpaWalletRepository;
    }

    @Override
    public WalletDTO findByUserId(String userId) {
        WalletEntity walletEntity = jpaWalletRepository.findByUserId(userId);
        return WalletDomainMapper.toWalletDomain(walletEntity);
    }

    @Override
    public void save(WalletDTO walletDTO) {
        WalletEntity entity = WalletDomainMapper.toWalletEntity(walletDTO);
        jpaWalletRepository.save(entity);
    }
}