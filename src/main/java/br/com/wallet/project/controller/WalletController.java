package br.com.wallet.project.controller;

import br.com.wallet.project.controller.request.*;
import br.com.wallet.project.controller.response.TransactionHistoryResponse;
import br.com.wallet.project.controller.response.TransactionResponse;
import br.com.wallet.project.controller.response.WalletResponse;
import br.com.wallet.project.domain.TransactionType;
import br.com.wallet.project.domain.service.WalletService;
import br.com.wallet.project.domain.service.WalletTransactionProcessorServiceMessage;
import br.com.wallet.project.mapper.TransactionRequestMapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@RestController
@Slf4j
@Validated
public class WalletController implements WalletControllerApi {
    private final WalletService walletService;
    private final WalletTransactionProcessorServiceMessage walletTransactionMessageProcessorService;

    public WalletController(WalletService walletService, WalletTransactionProcessorServiceMessage walletTransactionMessageProcessorService) {
        this.walletService = walletService;
        this.walletTransactionMessageProcessorService = walletTransactionMessageProcessorService;
    }

    @Override
    public ResponseEntity<WalletResponse> createWallet(WalletRequest walletRequest) {
        return ResponseEntity.ok().body(walletService.createWallet(walletRequest));
    }

    @Override
    public ResponseEntity<WalletResponse> retrieveBalance(WalletRequest walletRequest) {
        return ResponseEntity.ok().body(walletService.retrieveBalance(walletRequest));
    }

    @Override
    public ResponseEntity<TransactionResponse> depositFunds(@Valid @RequestBody TransactionOperationRequest transactionOperationRequest) {
        return ResponseEntity.ok().body(walletTransactionMessageProcessorService.processTransactionMessage(
                TransactionRequestMapper.mapTransactionOperationRequestToTransactionRequest(
                    transactionOperationRequest, TransactionType.DEPOSIT), transactionOperationRequest.getIdempotencyId()));
    }

    @Override
    public ResponseEntity<TransactionResponse> withdrawFunds(@Valid @RequestBody TransactionOperationRequest transactionOperationRequest) {
        return ResponseEntity.ok().body(walletTransactionMessageProcessorService.processTransactionMessage(
                TransactionRequestMapper.mapTransactionOperationRequestToTransactionRequest(
                    transactionOperationRequest, TransactionType.WITHDRAW), transactionOperationRequest.getIdempotencyId()));
    }

    @Override
    public ResponseEntity<TransactionResponse> transferFunds(@Valid @RequestBody TransferRequest transferRequest) {
        return ResponseEntity.ok().body(walletTransactionMessageProcessorService.processTransactionMessage(
                TransactionRequestMapper.mapTransferRequestIntoTransactionRequest(
                    transferRequest), transferRequest.getIdempotencyId()));
    }

    @Override
    public ResponseEntity<TransactionResponse> anyOperation(@Valid @RequestBody GenericRequest genericRequest) {
        return ResponseEntity.ok().body(walletTransactionMessageProcessorService.processTransactionMessage(
                TransactionRequestMapper.mapGenericRequestToTransactionRequest(
                        genericRequest), genericRequest.getIdempotencyId()));
    }

    @Override
    public ResponseEntity<List<TransactionHistoryResponse>> history(@Valid @RequestBody HistoryTransactionRequest historyTransactionRequest) {
        log.info("Fetching transaction history for user id {} on date {}",
                historyTransactionRequest.getUserId(), historyTransactionRequest.getDate());
        walletService.validateWallet(historyTransactionRequest.getUserId());
        return ResponseEntity.ok().body(walletService.retrieveBalanceHistory(historyTransactionRequest));
    }

}
