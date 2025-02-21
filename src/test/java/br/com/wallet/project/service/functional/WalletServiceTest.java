package br.com.wallet.project.service.functional;

import br.com.wallet.project.controller.request.WalletRequest;
import br.com.wallet.project.domain.TransactionType;
import br.com.wallet.project.domain.request.TransactionRequest;
import br.com.wallet.project.repositoy.TransactionRepository;
import br.com.wallet.project.repositoy.WalletRepository;
import br.com.wallet.project.repositoy.model.Transaction;
import br.com.wallet.project.repositoy.model.Wallet;
import br.com.wallet.project.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.DockerComposeContainer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class WalletServiceTest implements TestContainerSetup {

    @Autowired
    WalletService walletService;
    @Autowired
    WalletRepository walletRepository;
    @Autowired
    TransactionRepository transactionRepository;

    private static final DockerComposeContainer<?> environment = new WalletServiceTest().getContainer();

    @Test
    @Transactional("transactionManager")
    void testCreateWallet() {
        String userId = UUID.randomUUID().toString();
        walletService.createWallet(buildWalletRequest(userId));
        assertNotNull(walletRepository.findByUserId(userId));
    }

    @Test
    @Transactional("transactionManager")
    void testDepositFundsForUser() throws InterruptedException {
        String userId = UUID.randomUUID().toString();
        UUID transactionId = UUID.randomUUID();
        BigDecimal funds = BigDecimal.valueOf(100.0);
        walletService.createWallet(buildWalletRequest(userId));
        TransactionRequest transactionRequest =
                buildTransactionRequest(
                        userId,
                        transactionId,
                        funds,
                        TransactionType.DEPOSIT,
                        null,
                        null);
        walletService.transactionOperation(transactionRequest);
        Thread.sleep(2000);
        Wallet walletAfterOperation = walletRepository.findByUserId(userId);
        assertEquals(BigDecimal.valueOf(100.0).setScale(2, RoundingMode.HALF_DOWN), walletAfterOperation.getBalance());
        assertNotNull(transactionRepository.findByTransactionTrackId(transactionId));
    }

    @Test
    @Transactional("transactionManager")
    void shouldWithdrawFundsAfterDeposit() throws InterruptedException {
        String userId = UUID.randomUUID().toString();
        UUID transactionDepositId = UUID.randomUUID();
        UUID transactionWithdrawId = UUID.randomUUID();
        BigDecimal funds = BigDecimal.valueOf(100.0);

        walletService.createWallet(buildWalletRequest(userId));

        TransactionRequest transactionDepositRequest =
                buildTransactionRequest(
                        userId,
                        transactionDepositId,
                        funds,
                        TransactionType.DEPOSIT,
                        null,
                        null);
        walletService.transactionOperation(transactionDepositRequest);

        TransactionRequest transactionWithdrawRequest =
                buildTransactionRequest(
                        userId,
                        transactionWithdrawId,
                        BigDecimal.TEN,
                        TransactionType.WITHDRAW,
                        null,
                        null);
        walletService.transactionOperation(transactionWithdrawRequest);

        Thread.sleep(2000);
        Wallet walletAfterOperation = walletRepository.findByUserId(userId);
        assertEquals(BigDecimal.valueOf(90.0).setScale(2, RoundingMode.HALF_DOWN), walletAfterOperation.getBalance());
        assertNotNull(transactionRepository.findByTransactionTrackId(transactionDepositId));
        assertNotNull(transactionRepository.findByTransactionTrackId(transactionWithdrawId));
    }

    @Test
    @Transactional("transactionManager")
    void shouldTransferFromWalletToAnother() throws InterruptedException {
        String fromUserId = UUID.randomUUID().toString();
        String toUserId = UUID.randomUUID().toString();
        UUID transactionDepositId = UUID.randomUUID();
        UUID transferTransactionId = UUID.randomUUID();
        BigDecimal funds = BigDecimal.valueOf(100.0);

        walletService.createWallet(buildWalletRequest(fromUserId));
        walletService.createWallet(buildWalletRequest(toUserId));

        TransactionRequest transactionDepositRequest =
                buildTransactionRequest(
                        fromUserId,
                        transactionDepositId,
                        funds,
                        TransactionType.DEPOSIT,
                        null,
                        null);
        walletService.transactionOperation(transactionDepositRequest);

        TransactionRequest transferRequest =
                buildTransactionRequest(
                        null,
                        transferTransactionId,
                        BigDecimal.valueOf(10),
                        TransactionType.TRANSFER,
                        fromUserId,
                        toUserId);
        walletService.transactionOperation(transferRequest);

        Thread.sleep(2000);

        Wallet walletAfterOperationFromUserId = walletRepository.findByUserId(fromUserId);
        assertEquals(BigDecimal.valueOf(90.00).setScale(2, RoundingMode.HALF_DOWN), walletAfterOperationFromUserId.getBalance());

        Wallet walletAfterOperationToUserId = walletRepository.findByUserId(toUserId);
        assertEquals(BigDecimal.valueOf(10.00).setScale(2, RoundingMode.HALF_DOWN), walletAfterOperationToUserId.getBalance());

        assertNotNull(transactionRepository.findByTransactionTrackId(transactionDepositId));
        assertEquals(2, transactionRepository.findByTransactionTrackId(transferTransactionId).size());
    }

    private static WalletRequest buildWalletRequest(String userId) {
        return new WalletRequest(userId);
    }

    private static TransactionRequest buildTransactionRequest(
            String userId,
            UUID transactionId,
            BigDecimal amount,
            TransactionType transactionType,
            String fromUserWalletId,
            String toUserWalletId
    ) {
        return TransactionRequest
                .builder()
                .userId(userId)
                .transactionId(transactionId)
                .amount(amount)
                .transactionType(transactionType)
                .fromUserWalletId(fromUserWalletId)
                .toUserWalletId(toUserWalletId)
                .build();
    }

}

