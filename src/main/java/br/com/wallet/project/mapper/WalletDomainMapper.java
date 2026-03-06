package br.com.wallet.project.mapper;

import br.com.wallet.project.domain.dto.WalletDTO;
import br.com.wallet.project.domain.model.WalletEntity;

import java.math.BigDecimal;

public class WalletDomainMapper {

    private WalletDomainMapper() {}

    public static WalletDTO toWalletDomain(WalletEntity walletEntity) {
        if (walletEntity == null) return null;
        return WalletDTO.builder()
                .id(walletEntity.getId())
                .userId(walletEntity.getUserId())
                .balance(walletEntity.getBalance())
                .version(walletEntity.getVersion())
                .build();
    }

    public static WalletEntity toWalletEntity(WalletDTO walletDTO) {
        return WalletEntity.builder()
                .id(walletDTO.getId())
                .userId(walletDTO.getUserId())
                .balance(walletDTO.getBalance())
                .version(walletDTO.getVersion())
                .build();
    }

    public static WalletDTO newWalletDomain(String userId) {
        return WalletDTO.builder()
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .build();
    }
}
