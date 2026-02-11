package br.com.wallet.project.service.functional;

import br.com.wallet.project.controller.request.WalletRequest;
import br.com.wallet.project.domain.TransactionType;
import br.com.wallet.project.domain.request.TransactionRequest;
import br.com.wallet.project.infrastructure.persistence.jpa.repository.JpaTransactionRepository;
import br.com.wallet.project.infrastructure.persistence.jpa.repository.JpaWalletRepository;
import br.com.wallet.project.domain.model.Wallet;
import br.com.wallet.project.domain.service.WalletService;
import br.com.wallet.project.util.MoneyUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class WalletServiceTest implements TestContainerSetup {

    @Autowired
    WalletService walletService;
    @Autowired
    JpaWalletRepository jpaWalletRepository;
    @Autowired
    JpaTransactionRepository jpaTransactionRepository;

    @Test
    @Transactional("transactionManager")
    void testCreateWallet() {
        String userId = UUID.randomUUID().toString();
        walletService.createWallet(buildWalletRequest(userId));
        assertNotNull(jpaWalletRepository.findByUserId(userId));
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
        walletService.transactionProcessor(transactionRequest);
        Thread.sleep(2000);
        Wallet walletAfterOperation = jpaWalletRepository.findByUserId(userId);
        assertEquals(MoneyUtil.format(BigDecimal.valueOf(100.0)), walletAfterOperation.getBalance());
        assertNotNull(jpaTransactionRepository.findByTransactionTrackId(transactionId));
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
        walletService.transactionProcessor(transactionDepositRequest);

        TransactionRequest transactionWithdrawRequest =
                buildTransactionRequest(
                        userId,
                        transactionWithdrawId,
                        BigDecimal.TEN,
                        TransactionType.WITHDRAW,
                        null,
                        null);
        walletService.transactionProcessor(transactionWithdrawRequest);

        Thread.sleep(2000);
        Wallet walletAfterOperation = jpaWalletRepository.findByUserId(userId);
        assertEquals(MoneyUtil.format(BigDecimal.valueOf(90.0)), walletAfterOperation.getBalance());
        assertNotNull(jpaTransactionRepository.findByTransactionTrackId(transactionDepositId));
        assertNotNull(jpaTransactionRepository.findByTransactionTrackId(transactionWithdrawId));
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
        walletService.transactionProcessor(transactionDepositRequest);

        TransactionRequest transferRequest =
                buildTransactionRequest(
                        null,
                        transferTransactionId,
                        BigDecimal.valueOf(10),
                        TransactionType.TRANSFER,
                        fromUserId,
                        toUserId);
        walletService.transactionProcessor(transferRequest);

        Thread.sleep(2000);

        Wallet walletAfterOperationFromUserId = jpaWalletRepository.findByUserId(fromUserId);
        assertEquals(MoneyUtil.format(BigDecimal.valueOf(90.00)), walletAfterOperationFromUserId.getBalance());

        Wallet walletAfterOperationToUserId = jpaWalletRepository.findByUserId(toUserId);
        assertEquals(MoneyUtil.format(BigDecimal.valueOf(10.00)), walletAfterOperationToUserId.getBalance());

        assertNotNull(jpaTransactionRepository.findByTransactionTrackId(transactionDepositId));
        assertEquals(2, jpaTransactionRepository.findByTransactionTrackId(transferTransactionId).size());
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

