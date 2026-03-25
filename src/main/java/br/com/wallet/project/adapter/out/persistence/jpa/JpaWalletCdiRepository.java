package br.com.wallet.project.adapter.out.persistence.jpa;

import br.com.wallet.project.adapter.out.persistence.entity.WalletCdiEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaWalletCdiRepository extends JpaRepository<WalletCdiEntity, Long> {

    @Query("SELECT w FROM WalletCdiEntity w WHERE w.walletEntity.userId = :userId ORDER BY w.calculatedAt DESC")
    List<WalletCdiEntity> findByWalletEntityUserId(@Param("userId") String userId);
}
