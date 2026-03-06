package br.com.wallet.project.adapter.in.web.controller;
import br.com.wallet.project.adapter.in.web.request.*;
import br.com.wallet.project.adapter.in.web.response.*;
import br.com.wallet.project.application.port.in.TransactionUseCase;
import br.com.wallet.project.application.port.in.WalletUseCase;
import br.com.wallet.project.shared.mapper.TransactionCommandMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@Slf4j
@Validated
@RequiredArgsConstructor
public class WalletController implements WalletControllerApi {
    private final WalletUseCase walletUseCase;
    private final TransactionUseCase transactionUseCase;
    @Override
    public ResponseEntity<WalletResponse> createWallet(@Valid @RequestBody WalletRequest request) {
        return ResponseEntity.ok(walletUseCase.createWallet(request));
    }
    @Override
    public ResponseEntity<WalletResponse> retrieveBalance(@Valid @RequestBody WalletRequest request) {
        return ResponseEntity.ok(walletUseCase.retrieveBalance(request));
    }
    @Override
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody TransactionOperationRequest request) {
        return ResponseEntity.ok(transactionUseCase.publishTransaction(TransactionCommandMapper.fromDepositRequest(request), request.getIdempotencyId()));
    }
    @Override
    public ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody TransactionOperationRequest request) {
        return ResponseEntity.ok(transactionUseCase.publishTransaction(TransactionCommandMapper.fromWithdrawRequest(request), request.getIdempotencyId()));
    }
    @Override
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(transactionUseCase.publishTransaction(TransactionCommandMapper.fromTransferRequest(request), request.getIdempotencyId()));
    }
    @Override
    public ResponseEntity<TransactionResponse> anyOperation(@Valid @RequestBody GenericTransactionRequest request) {
        return ResponseEntity.ok(transactionUseCase.publishTransaction(TransactionCommandMapper.fromGenericRequest(request), request.getIdempotencyId()));
    }
    @Override
    public ResponseEntity<List<TransactionHistoryResponse>> history(@Valid @RequestBody HistoryTransactionRequest request) {
        log.info("Fetching history for user {} on date {}", request.getUserId(), request.getDate());
        walletUseCase.validateWallet(request.getUserId());
        return ResponseEntity.ok(walletUseCase.retrieveTransactionHistory(request));
    }
}
