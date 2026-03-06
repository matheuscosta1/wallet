package br.com.wallet.project.infrastructure.persistence.jpa;

import br.com.wallet.project.domain.model.TransferEntity;
import br.com.wallet.project.infrastructure.persistence.TransferRepository;
import br.com.wallet.project.infrastructure.persistence.jpa.repository.JpaTransferRepository;
import org.springframework.stereotype.Repository;

@Repository
public class TransferRepositoryJpaAdapter implements TransferRepository {
    private final JpaTransferRepository jpaTransferRepository;

    public TransferRepositoryJpaAdapter(JpaTransferRepository jpaTransferRepository) {
        this.jpaTransferRepository = jpaTransferRepository;
    }

    @Override
    public void save(TransferEntity transferEntity) {
        jpaTransferRepository.save(transferEntity);
    }
}