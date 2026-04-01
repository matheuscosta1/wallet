package br.com.wallet.project.functional.cucumber.steps;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;

import br.com.wallet.project.adapter.in.web.request.WalletRequest;
import br.com.wallet.project.adapter.in.web.response.WalletResponse;
import br.com.wallet.project.application.port.out.WalletRepository;
import br.com.wallet.project.application.service.WalletService;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;

public class WalletSteps {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    private WalletResponse response;

    @Dado("que não exista carteira para o usuáriorio {string}")
    public void naoExistaCarteira(String userId) {
        if (walletRepository.findByUserId(userId) != null) {
            throw new IllegalStateException("Pré-condição falhou: carteira já existe para userId=" + userId);
        }
    }

    @Quando("eu criar a carteira para o usuárioário {string}")
    public void criarCarteira(String userId) {
        WalletRequest request = WalletRequest.builder().userId(userId).build();
        response = walletService.createWallet(request);
    }

    @Entao("a carteira deve existir com o usuário {string} e saldo {string}")
    public void validarCarteira(String userId, String saldoEsperado) {
        assertEquals(userId, response.getUserId());
        assertEquals(new BigDecimal(saldoEsperado), response.getBalance());
    }

    @Dado("que exista carteira para o usuário {string}")
    public void existaCarteira(String userId) {
        if (walletRepository.findByUserId(userId) == null) {
            WalletRequest request = WalletRequest.builder().userId(userId).build();
            walletService.createWallet(request);
        }
    }

    @Quando("eu tentar criar a carteira para o usuário {string}")
    public void tentarCriarCarteira(String userId) {
        try {
            WalletRequest request = WalletRequest.builder().userId(userId).build();
            response = walletService.createWallet(request);
        } catch (Exception e) {
            response = null; // Indicate error
        }
    }

    @Entao("deve retornar erro de carteira já existe")
    public void erroCarteiraJaExiste() {
        assertNull(response);
    }
}
