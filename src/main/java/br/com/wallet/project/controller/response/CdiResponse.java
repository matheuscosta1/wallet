package br.com.wallet.project.controller.response;

import java.math.BigDecimal;

public class CdiResponse {
    private BigDecimal calculatedValue;

    // Construtor vazio (necessário para frameworks como o Jackson)
    public CdiResponse() {
    }

    // Construtor para facilitar a criação no Service
    public CdiResponse(BigDecimal calculatedValue) {
        this.calculatedValue = calculatedValue;
    }

    // Getters e Setters
    public BigDecimal getCalculatedValue() {
        return calculatedValue;
    }

    public void setCalculatedValue(BigDecimal calculatedValue) {
        this.calculatedValue = calculatedValue;
    }
}
