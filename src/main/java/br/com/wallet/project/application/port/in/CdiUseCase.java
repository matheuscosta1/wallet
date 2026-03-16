package br.com.wallet.project.application.port.in;

import br.com.wallet.project.adapter.in.web.request.CdiRequest;
import br.com.wallet.project.adapter.in.web.response.CdiResponse;

import java.util.List;

/**
 * Driving port: CDI (Certificado de Depósito Interbancário) use cases.
 * Exposes CDI yield calculation and history retrieval to the outside world.
 */
public interface CdiUseCase {

    /**
     * Calculates CDI yield for the given wallet and persists the result.
     * Applies the daily CDI rate to the current wallet balance.
     */
    CdiResponse calculateCdi(CdiRequest request);

    /**
     * Returns the full CDI calculation history for a given wallet.
     */
    List<CdiResponse> getCdiHistory(String userId);
}
