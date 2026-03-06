package br.com.wallet.project.application.port.in;
import br.com.wallet.project.adapter.in.web.request.HistoryTransactionRequest;
import br.com.wallet.project.adapter.in.web.request.WalletRequest;
import br.com.wallet.project.adapter.in.web.response.TransactionHistoryResponse;
import br.com.wallet.project.adapter.in.web.response.WalletResponse;
import br.com.wallet.project.domain.model.Wallet;
import java.util.List;
import java.util.UUID;
/** Driving port: wallet use cases exposed to the outside world. */
public interface WalletUseCase {
    WalletResponse createWallet(WalletRequest request);
    WalletResponse retrieveBalance(WalletRequest request);
    List<TransactionHistoryResponse> retrieveTransactionHistory(HistoryTransactionRequest request);
    Wallet validateWallet(String userId);
    Wallet validateWallet(String userId, UUID transactionId);
}
