package br.com.wallet.project.infrastructure.persistence.jpa.repository;

import br.com.wallet.project.domain.model.TransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaTransferRepository extends JpaRepository<TransferEntity, Long> {
}
