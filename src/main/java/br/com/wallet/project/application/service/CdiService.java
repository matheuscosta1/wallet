package br.com.wallet.project.application.service;

import br.com.wallet.project.adapter.in.web.request.CdiRequest;
import br.com.wallet.project.adapter.in.web.response.CdiResponse;
import br.com.wallet.project.application.port.in.CdiUseCase;
import br.com.wallet.project.application.port.in.WalletUseCase;
import br.com.wallet.project.application.port.out.WalletCdiRepository;
import br.com.wallet.project.application.port.out.WalletRepository;
import br.com.wallet.project.domain.model.Wallet;
import br.com.wallet.project.domain.model.WalletCdi;
import br.com.wallet.project.shared.mapper.WalletCdiResponseMapper;
import br.com.wallet.project.shared.util.MoneyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CdiService implements CdiUseCase {

    private final WalletUseCase walletUseCase;
    private final WalletRepository walletRepository;
    private final WalletCdiRepository walletCdiRepository;

    /**
     * Applies daily CDI yield to the wallet balance.
     *
     * Formula: yieldAmount = balanceBefore * cdiRate
     *          balanceAfter = balanceBefore + yieldAmount
     *
     * The yield is credited directly to the wallet balance and the
     * calculation is persisted for auditing purposes.
     */
    @Override
    @Transactional("transactionManager")
    public CdiResponse calculateCdi(CdiRequest request) {
        log.info("Calculating CDI for userId={} with rate={}", request.getUserId(), request.getCdiRate());

        Wallet wallet = walletUseCase.validateWallet(request.getUserId());

        BigDecimal balanceBefore = MoneyUtil.format(wallet.getBalance());
        BigDecimal cdiRate = request.getCdiRate();

        BigDecimal yieldAmount = balanceBefore
                .multiply(cdiRate, MathContext.DECIMAL128)
                .setScale(2, RoundingMode.HALF_DOWN);

        BigDecimal balanceAfter = MoneyUtil.format(balanceBefore.add(yieldAmount));

        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);

        WalletCdi walletCdi = WalletCdi.builder()
                .userId(request.getUserId())
                .balanceBeforeCdi(balanceBefore)
                .balanceAfterCdi(balanceAfter)
                .cdiRate(cdiRate)
                .yieldAmount(yieldAmount)
                .calculatedAt(LocalDateTime.now())
                .build();

        WalletCdi saved = walletCdiRepository.save(walletCdi);

        log.info("CDI applied for userId={}: before={}, after={}, yield={}",
                request.getUserId(), balanceBefore, balanceAfter, yieldAmount);

        return WalletCdiResponseMapper.toResponse(saved);
    }

    @Override
    @Transactional("transactionManager")
    public List<CdiResponse> getCdiHistory(String userId) {
        log.info("Fetching CDI history for userId={}", userId);
        walletUseCase.validateWallet(userId);
        return walletCdiRepository.findByUserId(userId)
                .stream()
                .map(WalletCdiResponseMapper::toResponse)
                .collect(Collectors.toList());
    }
}
