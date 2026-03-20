package br.com.wallet.project.application.service;

import br.com.wallet.project.adapter.in.web.request.HistoryTransactionRequest;
import br.com.wallet.project.adapter.in.web.request.WalletRequest;
import br.com.wallet.project.adapter.in.web.response.TransactionHistoryResponse;
import br.com.wallet.project.adapter.in.web.response.WalletResponse;
import br.com.wallet.project.application.port.in.WalletUseCase;
import br.com.wallet.project.application.port.out.TransactionRepository;
import br.com.wallet.project.application.port.out.WalletEventRepository;
import br.com.wallet.project.application.port.out.WalletRepository;
import br.com.wallet.project.domain.exception.WalletDomainException;
import br.com.wallet.project.domain.exception.WalletErrors;
import br.com.wallet.project.domain.model.Transaction;
import br.com.wallet.project.domain.model.Wallet;
import br.com.wallet.project.shared.factory.WalletEventFactory;
import br.com.wallet.project.shared.mapper.TransactionHistoryMapper;
import br.com.wallet.project.shared.util.MoneyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService implements WalletUseCase {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    // ── Event sourcing ────────────────────────────────────────────────────────
    private final WalletEventRepository walletEventRepository;

    @Override
    @Transactional("transactionManager")
    public WalletResponse createWallet(WalletRequest request) {
        Wallet existing = walletRepository.findByUserId(request.getUserId());
        if (existing != null) {
            throw new WalletDomainException(
                    MessageFormat.format(WalletErrors.W0001.message(), request.getUserId()),
                    WalletErrors.W0001.name(), WalletErrors.W0001.group());
        }

        Wallet newWallet = Wallet.builder()
                .userId(request.getUserId())
                .balance(BigDecimal.ZERO)
                .build();
        walletRepository.save(newWallet);

        // ── Emit WALLET_CREATED event ─────────────────────────────────────────
        walletEventRepository.save(
                WalletEventFactory.walletCreated(request.getUserId(), null));

        return WalletResponse.builder()
                .userId(newWallet.getUserId())
                .balance(MoneyUtil.format(newWallet.getBalance()))
                .build();
    }

    @Override
    @Transactional("transactionManager")
    public WalletResponse retrieveBalance(WalletRequest request) {
        Wallet wallet = validateWallet(request.getUserId());
        return WalletResponse.builder()
                .userId(wallet.getUserId())
                .balance(MoneyUtil.format(wallet.getBalance()))
                .build();
    }

    @Override
    @Transactional("transactionManager")
    public List<TransactionHistoryResponse> retrieveTransactionHistory(
            HistoryTransactionRequest request) {
        validateWallet(request.getUserId());
        LocalDateTime start = LocalDateTime.of(request.getDate().toLocalDate(),
                LocalTime.MIDNIGHT);
        LocalDateTime end = LocalDateTime.of(request.getDate().toLocalDate(),
                LocalTime.MAX);
        List<Transaction> transactions =
                transactionRepository.findByDateRangeAndUserId(start, end,
                        request.getUserId());
        return transactions.stream()
                .map(TransactionHistoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional("transactionManager")
    public Wallet validateWallet(String userId) {
        Wallet wallet = walletRepository.findByUserId(userId);
        if (wallet == null) {
            log.error("Wallet does not exist for user id {}", userId);
            throw new WalletDomainException(
                    MessageFormat.format(WalletErrors.W0006.message(), userId),
                    WalletErrors.W0006.name(), WalletErrors.W0006.group());
        }
        return wallet;
    }

    @Override
    @Transactional("transactionManager")
    public Wallet validateWallet(String userId, UUID transactionId) {
        Wallet wallet = walletRepository.findByUserId(userId);
        if (wallet == null) {
            log.error("Wallet does not exist for user id {}. Transaction id {}.",
                    userId, transactionId);
            throw new WalletDomainException(
                    MessageFormat.format(WalletErrors.W0006.message(), userId),
                    WalletErrors.W0006.name(), WalletErrors.W0006.group());
        }
        return wallet;
    }
}
