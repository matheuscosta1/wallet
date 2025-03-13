package br.com.wallet.project.infrastructure.persistence.jpa;

import br.com.wallet.project.domain.model.Wallet;
import br.com.wallet.project.infrastructure.persistence.WalletPersistence;
import br.com.wallet.project.infrastructure.persistence.jpa.repository.JpaWalletRepository;
import org.springframework.stereotype.Repository;

@Repository
public class JpaWalletPersistence implements WalletPersistence {
    private final JpaWalletRepository jpaWalletRepository;

    public JpaWalletPersistence(JpaWalletRepository jpaWalletRepository) {
        this.jpaWalletRepository = jpaWalletRepository;
    }

    @Override
    public Wallet findByUserId(String userId) {
        return jpaWalletRepository.findByUserId(userId);
    }

    @Override
    public void save(Wallet wallet) {
        jpaWalletRepository.save(wallet);
    }
}