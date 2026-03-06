package br.com.wallet.project.domain.service;

import br.com.wallet.project.domain.dto.WalletDTO;
import br.com.wallet.project.infrastructure.persistence.WalletRepository;
import br.com.wallet.project.domain.enums.WalletErrors;
import br.com.wallet.project.exception.WalletException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.UUID;

@Service
@Slf4j
public abstract class WalletValidationService {
    private final WalletRepository walletRepository;

    public WalletValidationService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Transactional("transactionManager")
    public WalletDTO validateWallet(String userId) {
        WalletDTO wallet = walletRepository.findByUserId(userId);
        if (wallet == null) {
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
    public WalletDTO validateWallet(String userId, UUID transactionId) {
        WalletDTO wallet = walletRepository.findByUserId(userId);
        if (wallet == null) {
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