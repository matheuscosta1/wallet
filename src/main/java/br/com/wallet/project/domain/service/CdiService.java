package br.com.wallet.project.domain.service;

import br.com.wallet.project.controller.request.WalletRequest;
import br.com.wallet.project.controller.response.CdiResponse;
import br.com.wallet.project.controller.response.WalletResponse;
import br.com.wallet.project.domain.model.Wallet;
import br.com.wallet.project.domain.model.WalletCdi;
import br.com.wallet.project.domain.model.WalletCdiEntitiy;
import br.com.wallet.project.infrastructure.persistence.WalletPersistence;
import br.com.wallet.project.infrastructure.persistence.jpa.WalletCdiRepository;
import br.com.wallet.project.infrastructure.persistence.jpa.repository.JpaWalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class CdiService {

    private final WalletService walletService;
    private final WalletCdiRepository walletCdiRepository;

    public CdiService(WalletService walletService, WalletCdiRepository walletCdiRepository) {
        this.walletService = walletService;
        this.walletCdiRepository = walletCdiRepository;
    }

    public CdiResponse calculateCdi(WalletRequest walletRequest) {
        Wallet wallet = walletService.validateWallet(walletRequest.getUserId());
        WalletCdi walletCdi =
                WalletCdi.builder()
                .userId(walletRequest.getUserId())
                .balance(wallet.getBalance())
                .createdAt(LocalDateTime.now())
                .build();
        walletCdi.calculateCdi();

        walletCdiRepository.save(walletCdi);
        CdiResponse response = new CdiResponse();
        response.setCalculatedValue(walletCdi.getCdiValue());
        return response;

    }
}

