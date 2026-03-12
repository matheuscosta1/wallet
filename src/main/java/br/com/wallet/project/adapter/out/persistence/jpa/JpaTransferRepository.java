package br.com.wallet.project.adapter.out.persistence.jpa;

import br.com.wallet.project.adapter.out.persistence.entity.TransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaTransferRepository extends JpaRepository<TransferEntity, Long> {
}
