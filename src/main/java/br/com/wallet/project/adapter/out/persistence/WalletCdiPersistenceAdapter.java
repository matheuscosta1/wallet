package br.com.wallet.project.adapter.out.persistence;

import br.com.wallet.project.adapter.out.persistence.entity.WalletEntity;
import br.com.wallet.project.adapter.out.persistence.jpa.JpaWalletCdiRepository;
import br.com.wallet.project.adapter.out.persistence.jpa.JpaWalletRepository;
import br.com.wallet.project.application.port.out.WalletCdiRepository;
import br.com.wallet.project.domain.exception.WalletDomainException;
import br.com.wallet.project.domain.exception.WalletErrors;
import br.com.wallet.project.domain.model.WalletCdi;
import br.com.wallet.project.shared.mapper.WalletCdiEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

/** Driven adapter: implements WalletCdiRepository port using JPA + PostgreSQL. */
@Repository
@RequiredArgsConstructor
public class WalletCdiPersistenceAdapter implements WalletCdiRepository {

    private final JpaWalletCdiRepository jpaWalletCdiRepository;
    private final JpaWalletRepository jpaWalletRepository;

    @Override
    public WalletCdi save(WalletCdi walletCdi) {
        WalletEntity walletEntity = jpaWalletRepository.findByUserId(walletCdi.getUserId());
        if (walletEntity == null) {
            throw new WalletDomainException(
                MessageFormat.format(WalletErrors.W0006.message(), walletCdi.getUserId()),
                WalletErrors.W0006.name(),
                WalletErrors.W0006.group());
        }
        return WalletCdiEntityMapper.toDomain(
            jpaWalletCdiRepository.save(WalletCdiEntityMapper.toEntity(walletCdi, walletEntity)));
    }

    @Override
    public List<WalletCdi> findByUserId(String userId) {
        return jpaWalletCdiRepository.findByWalletEntityUserId(userId)
            .stream()
            .map(WalletCdiEntityMapper::toDomain)
            .collect(Collectors.toList());
    }
}
