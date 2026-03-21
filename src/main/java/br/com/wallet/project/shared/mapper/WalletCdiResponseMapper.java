package br.com.wallet.project.shared.mapper;

import br.com.wallet.project.adapter.in.web.response.CdiResponse;
import br.com.wallet.project.domain.model.WalletCdi;

public class WalletCdiResponseMapper {

    private WalletCdiResponseMapper() {}

    public static CdiResponse toResponse(WalletCdi walletCdi) {
        return CdiResponse.builder()
                .userId(walletCdi.getUserId())
                .balanceBeforeCdi(walletCdi.getBalanceBeforeCdi())
                .balanceAfterCdi(walletCdi.getBalanceAfterCdi())
                .cdiRate(walletCdi.getCdiRate())
                .yieldAmount(walletCdi.getYieldAmount())
                .calculatedAt(walletCdi.getCalculatedAt())
                .build();
    }
}
