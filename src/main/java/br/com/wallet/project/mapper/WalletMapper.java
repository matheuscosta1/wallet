package br.com.wallet.project.mapper;

import br.com.wallet.project.controller.request.WalletRequest;
import br.com.wallet.project.controller.response.TransactionHistoryResponse;
import br.com.wallet.project.domain.model.Transaction;
import br.com.wallet.project.domain.model.Wallet;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class WalletMapper {
    public static Wallet mapWalletRequestIntoWalletEntity(WalletRequest walletRequest) {
        return Wallet.builder().balance(BigDecimal.ZERO).userId(walletRequest.getUserId()).build();
    }
}
