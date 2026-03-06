package br.com.wallet.project.service.unit;

import br.com.wallet.project.adapter.in.web.request.HistoryTransactionRequest;
import br.com.wallet.project.adapter.in.web.request.WalletRequest;
import br.com.wallet.project.adapter.in.web.response.TransactionHistoryResponse;
import br.com.wallet.project.adapter.in.web.response.WalletResponse;
import br.com.wallet.project.application.port.out.TransactionRepository;
import br.com.wallet.project.application.port.out.WalletRepository;
import br.com.wallet.project.application.service.TransactionService;
import br.com.wallet.project.application.service.WalletService;
import br.com.wallet.project.domain.exception.WalletDomainException;
import br.com.wallet.project.domain.model.Transaction;
import br.com.wallet.project.domain.model.Wallet;
import br.com.wallet.project.shared.util.MoneyUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletEntityServiceTest {
    @Mock
    WalletRepository walletRepository;
    @Mock
    TransactionRepository transactionRepository;
    @Mock
    TransactionService transactionService;
    @InjectMocks
    WalletService walletService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateWallet() {
        when(walletRepository.findByUserId(anyString())).thenReturn(null);
        when(transactionRepository.save(any())).thenReturn(buildTransactionEntity());

        WalletResponse result = walletService.createWallet(buildWalletRequest("userId"));
        assertNotNull(result);
        assertEquals("userId", result.getUserId());
    }

    @Test
    void testRetrieveBalance() {
        when(walletRepository.findByUserId(anyString())).thenReturn(buildWalletEntity());
        WalletResponse result = walletService.retrieveBalance(buildWalletRequest("userId"));
        assertNotNull(result);
        assertEquals("userId", result.getUserId());
        assertEquals(MoneyUtil.format(BigDecimal.TEN), result.getBalance());
    }

    @Test
    void testRetrieveBalanceHistory() {
        when(walletRepository.findByUserId(anyString())).thenReturn(buildWalletEntity());
        when(transactionRepository.findByDateRangeAndUserId(any(LocalDateTime.class), any(LocalDateTime.class), anyString())).thenReturn(List.of(buildTransactionEntity()));
        List<TransactionHistoryResponse> result = walletService.retrieveTransactionHistory(new HistoryTransactionRequest("userId", LocalDateTime.of(2025, Month.FEBRUARY, 21, 19, 28, 8)));
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("userId", result.getFirst().getUserId());
    }

    @Test
    void testValidateWallet() {
        when(walletRepository.findByUserId(anyString())).thenReturn(buildWalletEntity());
        Wallet result = walletService.validateWallet("userId");
        assertEquals("userId", result.getUserId());
        assertEquals(BigDecimal.TEN, result.getBalance());
    }

    @Test
    void shouldThrowErrorWhenValidateWallet() {
        when(walletRepository.findByUserId(anyString())).thenReturn(null);
        assertThrows(WalletDomainException.class, () -> walletService.validateWallet("userId"));
    }

    @Test
    void testValidateWallet2() {
        when(walletRepository.findByUserId(anyString())).thenReturn(buildWalletEntity());
        Wallet result = walletService.validateWallet("userId", UUID.randomUUID());
        assertEquals("userId", result.getUserId());
        assertEquals(BigDecimal.TEN, result.getBalance());
    }

    @Test
    void shouldThrowErrorWhenValidateWallet2() {
        when(walletRepository.findByUserId(anyString())).thenReturn(null);
        assertThrows(WalletDomainException.class, () -> walletService.validateWallet("userId", UUID.randomUUID()));
    }

    private static WalletRequest buildWalletRequest(String userId) {
        return new WalletRequest(userId);
    }

    private static Wallet buildWalletEntity() {
        return Wallet
                .builder()
                .userId("userId")
                .balance(BigDecimal.TEN)
                .id(1L)
                .version(1L)
                .build();
    }

    private static Transaction buildTransactionEntity() {
        return Transaction
                .builder()
                .timestamp(LocalDateTime.now())
                .wallet(buildWalletEntity())
                .transactionTrackId(UUID.randomUUID())
                .amount(BigDecimal.TEN)
                .balanceAfterTransaction(BigDecimal.TEN)
                .balanceBeforeTransaction(BigDecimal.ZERO)
                .build();
    }

}

