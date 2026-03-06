package br.com.wallet.project.infrastructure.persistence;

import br.com.wallet.project.domain.model.TransferEntity;

public interface TransferRepository {
    void save(TransferEntity transferEntity);
}