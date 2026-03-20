package br.com.wallet.project.unit;

import br.com.wallet.project.application.command.TransactionCommand;
import br.com.wallet.project.domain.event.WalletEvent;
import br.com.wallet.project.domain.event.WalletEventType;
import br.com.wallet.project.domain.model.TransactionMessage;
import br.com.wallet.project.domain.model.Wallet;
import br.com.wallet.project.domain.model.enums.TransactionType;
import br.com.wallet.project.shared.factory.WalletEventFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WalletEventFactoryTest {

    // ── Builders ──────────────────────────────────────────────────────────────

    private static final UUID TRANSACTION_ID = UUID.randomUUID();

    private static TransactionCommand depositCmd() {
        return TransactionCommand.builder()
                .userId("alice")
                .transactionId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .transactionType(TransactionType.DEPOSIT)
                .idempotencyId(UUID.randomUUID())
                .build();
    }

    private static TransactionCommand withdrawCmd() {
        return TransactionCommand.builder()
                .userId("alice")
                .transactionId(UUID.randomUUID())
                .amount(new BigDecimal("40.00"))
                .transactionType(TransactionType.WITHDRAW)
                .idempotencyId(UUID.randomUUID())
                .build();
    }

    private static TransactionCommand transferCmd() {
        return TransactionCommand.builder()
                .fromUserWalletId("alice")
                .toUserWalletId("bob")
                .transactionId(TRANSACTION_ID)
                .amount(new BigDecimal("50.00"))
                .transactionType(TransactionType.TRANSFER)
                .idempotencyId(UUID.randomUUID())
                .build();
    }

    private static Wallet wallet(String userId, BigDecimal balance) {
        return Wallet.builder().id(1L).userId(userId).balance(balance).version(1L).build();
    }

    private static TransactionMessage depositMsg() {
        return TransactionMessage.builder()
                .transactionId(UUID.randomUUID())
                .idempotencyId(UUID.randomUUID())
                .userId("alice")
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.DEPOSIT)
                .build();
    }

    private static TransactionMessage withdrawMsg() {
        return TransactionMessage.builder()
                .transactionId(UUID.randomUUID())
                .idempotencyId(UUID.randomUUID())
                .userId("alice")
                .amount(new BigDecimal("40.00"))
                .type(TransactionType.WITHDRAW)
                .build();
    }

    private static TransactionMessage transferMsg() {
        return TransactionMessage.builder()
                .transactionId(TRANSACTION_ID)
                .idempotencyId(UUID.randomUUID())
                .fromUserId("alice")
                .toUserId("bob")
                .amount(new BigDecimal("50.00"))
                .type(TransactionType.TRANSFER)
                .build();
    }

    // ── walletCreated ─────────────────────────────────────────────────────────

    @Test
    void walletCreated_shouldSetCorrectFields() {
        WalletEvent event = WalletEventFactory.walletCreated("alice", null);
        assertEquals(WalletEventType.WALLET_CREATED, event.getEventType());
        assertEquals("WALLET", event.getAggregateType());
        assertEquals("alice",  event.getAggregateId());
        assertNotNull(event.getEventId());
        assertNotNull(event.getOccurredAt());
        assertEquals("alice", event.getPayload().get("userId"));
    }

    // ── depositRequested ──────────────────────────────────────────────────────

    @Test
    void depositRequested_shouldBelongToWalletAggregate() {
        TransactionCommand cmd = depositCmd();
        WalletEvent event = WalletEventFactory.depositRequested(cmd);
        assertEquals(WalletEventType.DEPOSIT_REQUESTED, event.getEventType());
        assertEquals("WALLET", event.getAggregateType());
        assertEquals("alice",  event.getAggregateId());
        assertEquals("100.00", event.getPayload().get("amount"));
        assertEquals(cmd.getIdempotencyId(), event.getCorrelationId());
    }

    // ── depositCompleted ──────────────────────────────────────────────────────

    @Test
    void depositCompleted_shouldBelongToWalletAggregate() {
        TransactionCommand cmd = depositCmd();
        Wallet w = wallet("alice", new BigDecimal("100.00"));
        WalletEvent event = WalletEventFactory.depositCompleted(cmd, w, BigDecimal.ZERO, null);
        assertEquals(WalletEventType.DEPOSIT_COMPLETED, event.getEventType());
        assertEquals("WALLET", event.getAggregateType());
        assertEquals("alice",  event.getAggregateId());
    }

    @Test
    void depositCompleted_shouldIncludeBalanceSnapshots() {
        TransactionCommand cmd = depositCmd();
        Wallet w = wallet("alice", new BigDecimal("100.00"));
        WalletEvent event = WalletEventFactory.depositCompleted(cmd, w, new BigDecimal("0.00"), null);
        assertEquals("0.00",   event.getPayload().get("balanceBefore").toString());
        assertEquals("100.00", event.getPayload().get("balanceAfter").toString());
        assertNotNull(event.getPayload().get("walletId"));
    }

    // ── withdrawRequested ─────────────────────────────────────────────────────

    @Test
    void withdrawRequested_shouldBelongToWalletAggregate() {
        TransactionCommand cmd = withdrawCmd();
        WalletEvent event = WalletEventFactory.withdrawRequested(cmd);
        assertEquals(WalletEventType.WITHDRAW_REQUESTED, event.getEventType());
        assertEquals("WALLET", event.getAggregateType());
        assertEquals("alice",  event.getAggregateId());
        assertEquals("40.00",  event.getPayload().get("amount"));
    }

    // ── withdrawCompleted ─────────────────────────────────────────────────────

    @Test
    void withdrawCompleted_shouldBelongToWalletAggregate() {
        TransactionCommand cmd = withdrawCmd();
        Wallet w = wallet("alice", new BigDecimal("60.00"));
        WalletEvent event = WalletEventFactory.withdrawCompleted(cmd, w,
                new BigDecimal("100.00"), null);
        assertEquals(WalletEventType.WITHDRAW_COMPLETED, event.getEventType());
        assertEquals("WALLET", event.getAggregateType());
        assertEquals("alice",  event.getAggregateId());
        assertEquals("100.00", event.getPayload().get("balanceBefore").toString());
        assertEquals("60.00",  event.getPayload().get("balanceAfter").toString());
    }

    // ── transferRequested ─────────────────────────────────────────────────────

    @Test
    void transferRequested_shouldBelongToTransferAggregate() {
        TransactionCommand cmd = transferCmd();
        WalletEvent event = WalletEventFactory.transferRequested(cmd);
        assertEquals(WalletEventType.TRANSFER_REQUESTED, event.getEventType());
        assertEquals("TRANSFER",                   event.getAggregateType());
        assertEquals(TRANSACTION_ID.toString(),    event.getAggregateId());
    }

    @Test
    void transferRequested_shouldContainFromAndToUserIds() {
        WalletEvent event = WalletEventFactory.transferRequested(transferCmd());
        assertEquals("alice", event.getPayload().get("fromUserId"));
        assertEquals("bob",   event.getPayload().get("toUserId"));
    }

    // ── transferCompleted ─────────────────────────────────────────────────────

    @Test
    void transferCompleted_shouldBelongToTransferAggregate() {
        TransactionCommand cmd = transferCmd();
        WalletEvent event = WalletEventFactory.transferCompleted(cmd,
                new BigDecimal("100.00"), new BigDecimal("50.00"),
                new BigDecimal("0.00"),   new BigDecimal("50.00"), null);
        assertEquals(WalletEventType.TRANSFER_COMPLETED, event.getEventType());
        assertEquals("TRANSFER",                event.getAggregateType());
        assertEquals(TRANSACTION_ID.toString(), event.getAggregateId());
    }

    @Test
    void transferCompleted_shouldIncludeAllFourBalances() {
        WalletEvent event = WalletEventFactory.transferCompleted(transferCmd(),
                new BigDecimal("100.00"), new BigDecimal("50.00"),
                new BigDecimal("0.00"),   new BigDecimal("50.00"), null);
        assertEquals("100.00", event.getPayload().get("fromBalanceBefore"));
        assertEquals("50.00",  event.getPayload().get("fromBalanceAfter"));
        assertEquals("0.00",   event.getPayload().get("toBalanceBefore"));
        assertEquals("50.00",  event.getPayload().get("toBalanceAfter"));
    }

    // ── transferFailed ────────────────────────────────────────────────────────

    @Test
    void transferFailed_shouldBelongToTransferAggregate() {
        WalletEvent event = WalletEventFactory.transferFailed(transferCmd(),
                "Insufficient funds.", null);
        assertEquals(WalletEventType.TRANSFER_FAILED, event.getEventType());
        assertEquals("TRANSFER",                event.getAggregateType());
        assertEquals(TRANSACTION_ID.toString(), event.getAggregateId());
        assertEquals("Insufficient funds.",     event.getPayload().get("reason"));
    }

    // ── idempotencyDuplicateDetected ──────────────────────────────────────────

    @Test
    void idempotencyDuplicateDetected_deposit_shouldBelongToWalletAggregate() {
        TransactionCommand cmd = depositCmd();
        WalletEvent event = WalletEventFactory.idempotencyDuplicateDetected(cmd);
        assertEquals(WalletEventType.IDEMPOTENCY_DUPLICATE_DETECTED, event.getEventType());
        assertEquals("WALLET", event.getAggregateType());
        assertEquals("alice",  event.getAggregateId());
        assertEquals("DEPOSIT", event.getPayload().get("transactionType"));
    }

    @Test
    void idempotencyDuplicateDetected_transfer_shouldBelongToTransferAggregate() {
        TransactionCommand cmd = transferCmd();
        WalletEvent event = WalletEventFactory.idempotencyDuplicateDetected(cmd);
        assertEquals("TRANSFER",                event.getAggregateType());
        assertEquals(TRANSACTION_ID.toString(), event.getAggregateId());
        assertEquals("TRANSFER", event.getPayload().get("transactionType"));
    }


    // ── depositPermanentlyFailed ──────────────────────────────────────────────

    @Test
    void depositPermanentlyFailed_shouldSetCorrectEventType() {
        WalletEvent event = WalletEventFactory.depositPermanentlyFailed(depositMsg(), "Wallet not found", 4);
        assertEquals(WalletEventType.DEPOSIT_PERMANENTLY_FAILED, event.getEventType());
    }

    @Test
    void depositPermanentlyFailed_shouldBelongToWalletAggregate() {
        WalletEvent event = WalletEventFactory.depositPermanentlyFailed(depositMsg(), "error", 4);
        assertEquals("WALLET", event.getAggregateType());
        assertEquals("alice",  event.getAggregateId());
    }

    @Test
    void depositPermanentlyFailed_shouldContainReasonAndRetryCount() {
        WalletEvent event = WalletEventFactory.depositPermanentlyFailed(depositMsg(), "Wallet not found", 4);
        assertEquals("Wallet not found", event.getPayload().get("reason"));
        assertEquals(4, event.getPayload().get("retryCount"));
    }

    @Test
    void depositPermanentlyFailed_shouldContainTransactionType() {
        WalletEvent event = WalletEventFactory.depositPermanentlyFailed(depositMsg(), "any", 4);
        assertEquals("DEPOSIT", event.getPayload().get("transactionType"));
    }

    @Test
    void depositPermanentlyFailed_shouldIncludeAmount() {
        WalletEvent event = WalletEventFactory.depositPermanentlyFailed(depositMsg(), "any", 4);
        assertEquals("100.00", event.getPayload().get("amount"));
    }

    @Test
    void depositPermanentlyFailed_shouldUseIdempotencyIdAsCorrelationId() {
        TransactionMessage msg = depositMsg();
        WalletEvent event = WalletEventFactory.depositPermanentlyFailed(msg, "any", 4);
        assertEquals(msg.getIdempotencyId(), event.getCorrelationId());
    }

    // ── withdrawPermanentlyFailed ─────────────────────────────────────────────

    @Test
    void withdrawPermanentlyFailed_shouldSetCorrectEventType() {
        WalletEvent event = WalletEventFactory.withdrawPermanentlyFailed(withdrawMsg(), "Insufficient funds.", 4);
        assertEquals(WalletEventType.WITHDRAW_PERMANENTLY_FAILED, event.getEventType());
    }

    @Test
    void withdrawPermanentlyFailed_shouldBelongToWalletAggregate() {
        WalletEvent event = WalletEventFactory.withdrawPermanentlyFailed(withdrawMsg(), "error", 4);
        assertEquals("WALLET", event.getAggregateType());
        assertEquals("alice",  event.getAggregateId());
    }

    @Test
    void withdrawPermanentlyFailed_shouldContainReasonAndRetryCount() {
        WalletEvent event = WalletEventFactory.withdrawPermanentlyFailed(withdrawMsg(), "Insufficient funds.", 4);
        assertEquals("Insufficient funds.", event.getPayload().get("reason"));
        assertEquals(4, event.getPayload().get("retryCount"));
    }

    // ── transferPermanentlyFailed ─────────────────────────────────────────────

    @Test
    void transferPermanentlyFailed_shouldSetCorrectEventType() {
        WalletEvent event = WalletEventFactory.transferPermanentlyFailed(transferMsg(), "error", 4);
        assertEquals(WalletEventType.TRANSFER_PERMANENTLY_FAILED, event.getEventType());
    }

    @Test
    void transferPermanentlyFailed_shouldBelongToTransferAggregate() {
        WalletEvent event = WalletEventFactory.transferPermanentlyFailed(transferMsg(), "error", 4);
        assertEquals("TRANSFER",                event.getAggregateType());
        assertEquals(TRANSACTION_ID.toString(), event.getAggregateId());
    }

    @Test
    void transferPermanentlyFailed_shouldContainFromAndToUserIds() {
        WalletEvent event = WalletEventFactory.transferPermanentlyFailed(transferMsg(), "error", 4);
        assertEquals("alice", event.getPayload().get("fromUserId"));
        assertEquals("bob",   event.getPayload().get("toUserId"));
    }

    @Test
    void transferPermanentlyFailed_shouldContainReasonAndRetryCount() {
        WalletEvent event = WalletEventFactory.transferPermanentlyFailed(transferMsg(), "Source wallet not found", 4);
        assertEquals("Source wallet not found", event.getPayload().get("reason"));
        assertEquals(4, event.getPayload().get("retryCount"));
    }

    // ── common invariants ─────────────────────────────────────────────────────

    @Test
    void allEvents_shouldHaveUniqueEventIds() {
        TransactionCommand cmd = depositCmd();
        WalletEvent e1 = WalletEventFactory.depositRequested(cmd);
        WalletEvent e2 = WalletEventFactory.depositRequested(cmd);
        assertNotEquals(e1.getEventId(), e2.getEventId());
    }

    @Test
    void allEvents_shouldHaveOccurredAt() {
        assertNotNull(WalletEventFactory.walletCreated("alice", null).getOccurredAt());
    }

    @Test
    void allEvents_shouldHaveVersionOne() {
        assertEquals(1, WalletEventFactory.depositRequested(depositCmd()).getEventVersion());
        assertEquals(1, WalletEventFactory.transferRequested(transferCmd()).getEventVersion());
        assertEquals(1, WalletEventFactory.depositPermanentlyFailed(depositMsg(), "e", 4).getEventVersion());
        assertEquals(1, WalletEventFactory.withdrawPermanentlyFailed(withdrawMsg(), "e", 4).getEventVersion());
        assertEquals(1, WalletEventFactory.transferPermanentlyFailed(transferMsg(), "e", 4).getEventVersion());
    }

    @Test
    void walletEvents_shouldNeverHaveTransferAggregateType() {
        assertEquals("WALLET", WalletEventFactory.depositRequested(depositCmd()).getAggregateType());
        assertEquals("WALLET", WalletEventFactory.withdrawRequested(withdrawCmd()).getAggregateType());
        assertEquals("WALLET", WalletEventFactory.walletCreated("alice", null).getAggregateType());
    }

    @Test
    void transferEvents_shouldNeverHaveWalletAggregateType() {
        assertEquals("TRANSFER", WalletEventFactory.transferRequested(transferCmd()).getAggregateType());
        assertEquals("TRANSFER", WalletEventFactory.transferFailed(transferCmd(), "e", null).getAggregateType());
    }
}