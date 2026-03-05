package br.com.wallet.project.infrastructure.persistence.jpa;

import br.com.wallet.project.domain.model.Wallet;
import br.com.wallet.project.domain.model.WalletCdi;
import br.com.wallet.project.domain.model.WalletCdiEntitiy;
import br.com.wallet.project.infrastructure.persistence.WalletCdiPersistence;
import br.com.wallet.project.infrastructure.persistence.WalletPersistence;
import br.com.wallet.project.infrastructure.persistence.jpa.repository.JpaWalletCdiRepository;
import br.com.wallet.project.infrastructure.persistence.jpa.repository.JpaWalletRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class WalletCdiRepository implements WalletCdiPersistence {
    private final JpaWalletCdiRepository jpaWalletCdiRepository;

    public WalletCdiRepository(JpaWalletCdiRepository jpaWalletCdiRepository) {
        this.jpaWalletCdiRepository = jpaWalletCdiRepository;

    }

    @Override
    public void save(WalletCdi wallet) {
        WalletCdiEntitiy entity = WalletCdiEntitiy.builder().userId(wallet.getUserId()).balance(wallet.getBalance()).CdiValue(wallet.getCdiValue()).createdAt(LocalDateTime.now()).build();
        jpaWalletCdiRepository.save(entity);
    }
}