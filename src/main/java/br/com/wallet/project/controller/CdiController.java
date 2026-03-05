package br.com.wallet.project.controller;

import br.com.wallet.project.controller.request.WalletRequest;
import br.com.wallet.project.controller.response.CdiResponse;
import br.com.wallet.project.controller.response.WalletResponse;
import br.com.wallet.project.domain.service.CdiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/cdi")
public class CdiController {
    @Autowired
    private CdiService cdiService;

    @PostMapping("/calculate")
    public ResponseEntity<CdiResponse> getCdiCalculation(@RequestBody WalletRequest walletRequest) {
        CdiResponse response = cdiService.calculateCdi(walletRequest);
        return ResponseEntity.ok(response);
    }
}
