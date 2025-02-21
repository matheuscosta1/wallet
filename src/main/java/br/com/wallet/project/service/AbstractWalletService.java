package br.com.wallet.project.service;

import br.com.wallet.project.repositoy.WalletRepository;
import br.com.wallet.project.repositoy.model.Wallet;
import br.com.wallet.project.service.enums.WalletErrors;
import br.com.wallet.project.service.exception.WalletException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.UUID;

@Service
@Slf4j
public abstract class AbstractWalletService {
    private final WalletRepository walletRepository;

    @Autowired
    public AbstractWalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Transactional("transactionManager")
    public Wallet validateWallet(String userId) {
        Wallet wallet = walletRepository.findByUserId(userId);
        if(wallet == null) {
            log.error("Wallet does not exists for user id {}", userId);
            throw new WalletException(
                    MessageFormat.format(
                            WalletErrors.W0006.message(), userId),
                    WalletErrors.W0006.name(),
                    WalletErrors.W0006.group());
        }
        return wallet;
    }

    @Transactional("transactionManager")
    public Wallet validateWallet(String userId, UUID transactionId) {
        Wallet wallet = walletRepository.findByUserId(userId);
        if(wallet == null) {
            log.error("Wallet does not exists for user id {}. Transaction id {}.", userId, transactionId);
            throw new WalletException(
                    MessageFormat.format(
                            WalletErrors.W0006.message(), userId),
                    WalletErrors.W0006.name(),
                    WalletErrors.W0006.group());
        }
        return wallet;
    }

}