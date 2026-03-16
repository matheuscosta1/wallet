package br.com.wallet.project.adapter.in.web.controller;

import br.com.wallet.project.adapter.in.web.request.CdiRequest;
import br.com.wallet.project.adapter.in.web.response.CdiResponse;
import br.com.wallet.project.application.port.in.CdiUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
public class CdiController implements CdiControllerApi {

    private final CdiUseCase cdiUseCase;

    @Override
    public ResponseEntity<CdiResponse> calculateCdi(@Valid @RequestBody CdiRequest request) {
        log.info("POST /cdi/calculate for userId={}", request.getUserId());
        return ResponseEntity.ok(cdiUseCase.calculateCdi(request));
    }

    @Override
    public ResponseEntity<List<CdiResponse>> getCdiHistory(@NotBlank @PathVariable String userId) {
        log.info("GET /cdi/history/{}", userId);
        return ResponseEntity.ok(cdiUseCase.getCdiHistory(userId));
    }
}
