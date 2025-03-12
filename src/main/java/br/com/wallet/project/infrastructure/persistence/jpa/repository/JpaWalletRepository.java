package br.com.wallet.project.infrastructure.persistence.jpa.repository;

import br.com.wallet.project.domain.model.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface JpaWalletRepository extends JpaRepository<Wallet, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Wallet findByUserId(String userId);
}
