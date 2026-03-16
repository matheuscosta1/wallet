package br.com.wallet.project.adapter.in.web.controller;

import br.com.wallet.project.adapter.in.web.request.CdiRequest;
import br.com.wallet.project.adapter.in.web.response.CdiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "CDI")
@RequestMapping("cdi")
public interface CdiControllerApi {

    @Operation(summary = "Apply daily CDI yield to a wallet balance")
    @PostMapping("calculate")
    ResponseEntity<CdiResponse> calculateCdi(@Valid @RequestBody CdiRequest request);

    @Operation(summary = "Retrieve CDI calculation history for a wallet")
    @GetMapping("history/{userId}")
    ResponseEntity<List<CdiResponse>> getCdiHistory(
            @PathVariable @NotBlank String userId);
}
