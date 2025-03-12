package br.com.wallet.project.infrastructure.persistence.jpa.repository;

import br.com.wallet.project.domain.model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaTransferRepository extends JpaRepository<Transfer, Long> {
}
