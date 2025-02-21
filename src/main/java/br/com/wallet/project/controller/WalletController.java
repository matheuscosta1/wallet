package br.com.wallet.project.controller;

import br.com.wallet.project.controller.request.HistoryTransactionRequest;
import br.com.wallet.project.controller.request.TransactionOperationRequest;
import br.com.wallet.project.controller.request.TransferRequest;
import br.com.wallet.project.controller.request.WalletRequest;
import br.com.wallet.project.controller.response.TransactionHistoryResponse;
import br.com.wallet.project.controller.response.TransactionResponse;
import br.com.wallet.project.controller.response.WalletResponse;
import br.com.wallet.project.domain.TransactionMessage;
import br.com.wallet.project.domain.TransactionType;
import br.com.wallet.project.service.WalletService;
import br.com.wallet.project.service.WalletTransactionProducerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Tag(name = "Wallet")
@RestController
@Slf4j
@Validated
public class WalletController {

    private final WalletService walletService;
    private final WalletTransactionProducerService walletTransactionProducerService;

    @Autowired
    public WalletController(WalletService walletService, WalletTransactionProducerService walletTransactionProducerService) {
        this.walletService = walletService;
        this.walletTransactionProducerService = walletTransactionProducerService;
    }

    @Operation(summary = "Create wallet", description = "Create wallet for user")
    @PostMapping("creation")
    public ResponseEntity<WalletResponse> createWallet(@Valid @RequestBody WalletRequest walletRequest) {
        return ResponseEntity.ok().body(walletService.createWallet(walletRequest));
    }

    @Operation(summary = "Wallet balance funds", description = "Retrieve wallet balance")
    @GetMapping("balance")
    public ResponseEntity<WalletResponse> retrieveBalance(@Valid @RequestBody WalletRequest walletRequest) {
        return ResponseEntity.ok().body(walletService.retrieveBalance(walletRequest));
    }

    @Operation(summary = "Wallet deposit funds", description = "Deposit funds into wallet")
    @PostMapping("deposit")
    public ResponseEntity<TransactionResponse> depositFunds(@Valid @RequestBody TransactionOperationRequest transactionOperationRequest) {
        UUID transactionId = UUID.randomUUID();
        log.info("Deposit funds for user id {} and transaction id {}", transactionOperationRequest.getUserId(), transactionId);

        walletService.validateWallet(transactionOperationRequest.getUserId(), transactionId);

        TransactionMessage transactionMessage =
                buildTransactionMessage(
                        transactionOperationRequest.getUserId(),
                        transactionId,
                        TransactionType.DEPOSIT,
                        transactionOperationRequest.getAmount(),
                        null,
                        null
                );
        walletTransactionProducerService.sendMessage(transactionMessage);
        log.info("Deposit funds message was successfully sent to wallet transactions topic for user id {} and transaction id {}",
                transactionOperationRequest.getUserId(), transactionId);
        return ResponseEntity.ok().body(buildTransactionResponse(transactionMessage, TransactionType.DEPOSIT));
    }

    @Operation(summary = "Wallet withdraw funds", description = "Withdraw funds from wallet")
    @PostMapping("withdraw")
    public ResponseEntity<TransactionResponse> withdrawFunds(@Valid @RequestBody TransactionOperationRequest transactionOperationRequest) {
        UUID transactionId = UUID.randomUUID();
        log.info("Withdraw funds for user id {} and transaction id {}", transactionOperationRequest.getUserId(), transactionId);

        walletService.validateWallet(transactionOperationRequest.getUserId(), transactionId);

        TransactionMessage transactionMessage =
                buildTransactionMessage(
                        transactionOperationRequest.getUserId(),
                        transactionId,
                        TransactionType.WITHDRAW,
                        transactionOperationRequest.getAmount(),
                        null,
                        null
                );
        walletTransactionProducerService.sendMessage(transactionMessage);
        log.info("Withdraw funds message was successfully sent to wallet transactions topic for user id {} and transaction id {}",
                transactionOperationRequest.getUserId(), transactionId);
        return ResponseEntity.ok().body(buildTransactionResponse(transactionMessage, TransactionType.WITHDRAW));
    }

    @Operation(summary = "Wallet transfer funds", description = "Transfer funds from an user to another")
    @PostMapping("transfer")
    public ResponseEntity<TransactionResponse> transferFunds(@Valid @RequestBody TransferRequest transactionOperationRequest) {
        UUID transactionId = UUID.randomUUID();
        log.info("Transfer funds from user id {} to user id {} with transaction id {}", transactionOperationRequest.getFromUserId(), transactionOperationRequest.getToUserId(), transactionId);

        walletService.validateWallet(transactionOperationRequest.getFromUserId(), transactionId);
        walletService.validateWallet(transactionOperationRequest.getToUserId(), transactionId);

        TransactionMessage transactionMessage =
                buildTransactionMessage(
                        null,
                        transactionId,
                        TransactionType.TRANSFER,
                        transactionOperationRequest.getAmount(),
                        transactionOperationRequest.getFromUserId(),
                        transactionOperationRequest.getToUserId()
                );
        walletTransactionProducerService.sendMessage(transactionMessage);
        log.info("Transfer funds message was successfully sent to wallet transactions from user id {} to user id {} with transaction id {}", transactionOperationRequest.getFromUserId(), transactionOperationRequest.getToUserId(), transactionId);
        return ResponseEntity.ok().body(buildTransactionResponse(transactionMessage, TransactionType.TRANSFER));
    }

    @Operation(summary = "Wallet transaction history", description = "Transaction history by specified date")
    @GetMapping("history")
    public ResponseEntity<List<TransactionHistoryResponse>> history(@Valid @RequestBody HistoryTransactionRequest historyTransactionRequest) {
        log.info("Start to get transaction history from user id {} on date {}", historyTransactionRequest.getUserId(), historyTransactionRequest.getDate());
        walletService.validateWallet(historyTransactionRequest.getUserId());
        return ResponseEntity.ok().body(walletService.balanceHistory(historyTransactionRequest));
    }

    private static TransactionResponse buildTransactionResponse(TransactionMessage transactionMessage, TransactionType transactionType) {
        return TransactionResponse
                .builder()
                .transactionId(transactionMessage.getTransactionId())
                .userId(transactionMessage.getUserId())
                .amount(transactionMessage.getAmount().setScale(2,  RoundingMode.DOWN))
                .transactionType(transactionType)
                .build();
    }

    private TransactionMessage buildTransactionMessage(String userId, UUID transactionId, TransactionType transactionType, BigDecimal amount, String fromUserId, String toUserId) {
        return TransactionMessage
                .builder()
                .type(transactionType)
                .userId(userId)
                .transactionId(transactionId)
                .amount(amount)
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .build();
    }
}
