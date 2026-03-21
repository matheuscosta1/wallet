package br.com.wallet.project.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain model representing the CDI yield applied to a wallet balance.
 * Stores a snapshot of the balance before/after CDI calculation.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WalletCdi {

    private Long id;
    private String userId;
    private BigDecimal balanceBeforeCdi;
    private BigDecimal balanceAfterCdi;
    private BigDecimal cdiRate;
    private BigDecimal yieldAmount;
    private LocalDateTime calculatedAt;
}
