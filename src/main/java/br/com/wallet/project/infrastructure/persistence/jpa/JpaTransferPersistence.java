package br.com.wallet.project.infrastructure.persistence.jpa;

import br.com.wallet.project.domain.model.Transfer;
import br.com.wallet.project.infrastructure.persistence.TransferPersistence;
import br.com.wallet.project.infrastructure.persistence.jpa.repository.JpaTransferRepository;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTransferPersistence implements TransferPersistence {
    private final JpaTransferRepository jpaTransferRepository;

    public JpaTransferPersistence(JpaTransferRepository jpaTransferRepository) {
        this.jpaTransferRepository = jpaTransferRepository;
    }

    @Override
    public void save(Transfer transfer) {
        jpaTransferRepository.save(transfer);
    }
}