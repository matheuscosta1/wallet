package br.com.wallet.project.shared.mapper;

import br.com.wallet.project.adapter.out.persistence.entity.WalletCdiEntity;
import br.com.wallet.project.adapter.out.persistence.entity.WalletEntity;
import br.com.wallet.project.domain.model.WalletCdi;

public class WalletCdiEntityMapper {

    private WalletCdiEntityMapper() {}

    public static WalletCdi toDomain(WalletCdiEntity entity) {
        if (entity == null) return null;
        return WalletCdi.builder()
                .id(entity.getId())
                .userId(entity.getWalletEntity().getUserId())
                .balanceBeforeCdi(entity.getBalanceBeforeCdi())
                .balanceAfterCdi(entity.getBalanceAfterCdi())
                .cdiRate(entity.getCdiRate())
                .yieldAmount(entity.getYieldAmount())
                .calculatedAt(entity.getCalculatedAt())
                .build();
    }

    public static WalletCdiEntity toEntity(WalletCdi domain, WalletEntity walletEntity) {
        return WalletCdiEntity.builder()
                .walletEntity(walletEntity)
                .balanceBeforeCdi(domain.getBalanceBeforeCdi())
                .balanceAfterCdi(domain.getBalanceAfterCdi())
                .cdiRate(domain.getCdiRate())
                .yieldAmount(domain.getYieldAmount())
                .calculatedAt(domain.getCalculatedAt())
                .build();
    }
}
