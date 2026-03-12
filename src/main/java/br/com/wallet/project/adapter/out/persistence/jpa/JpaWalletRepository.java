package br.com.wallet.project.adapter.out.persistence.jpa;
import br.com.wallet.project.adapter.out.persistence.entity.WalletEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
public interface JpaWalletRepository extends JpaRepository<WalletEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    WalletEntity findByUserId(String userId);
}
