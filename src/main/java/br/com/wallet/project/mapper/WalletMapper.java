package br.com.wallet.project.mapper;

import br.com.wallet.project.controller.request.WalletRequest;
import br.com.wallet.project.domain.dto.WalletDTO;

import java.math.BigDecimal;

public class WalletMapper {
    public static WalletDTO mapWalletRequestIntoWalletDomain(WalletRequest walletRequest) {
        return WalletDTO.builder().balance(BigDecimal.ZERO).userId(walletRequest.getUserId()).build();
    }
}
