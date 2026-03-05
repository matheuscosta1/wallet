package br.com.wallet.project.infrastructure.persistence.jpa.repository;

import br.com.wallet.project.domain.model.Wallet;
import br.com.wallet.project.domain.model.WalletCdiEntitiy;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface JpaWalletCdiRepository extends JpaRepository<WalletCdiEntitiy, Long> {

}
