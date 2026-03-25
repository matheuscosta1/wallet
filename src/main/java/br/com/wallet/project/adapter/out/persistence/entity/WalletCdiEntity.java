package br.com.wallet.project.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "wallet_cdi",
    indexes = {
        @Index(name = "idx_wallet_cdi_wallet_id",    columnList = "wallet_id"),
        @Index(name = "idx_wallet_cdi_calculated_at", columnList = "calculated_at")
    }
)
public class WalletCdiEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "wallet_id", nullable = false)
    private WalletEntity walletEntity;

    @Column(name = "balance_before_cdi", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceBeforeCdi;

    @Column(name = "balance_after_cdi", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfterCdi;

    @Column(name = "cdi_rate", nullable = false, precision = 10, scale = 6)
    private BigDecimal cdiRate;

    @Column(name = "yield_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal yieldAmount;

    @Column(name = "calculated_at", nullable = false, updatable = false)
    private LocalDateTime calculatedAt;

    @PrePersist
    public void prePersist() {
        if (calculatedAt == null) {
            calculatedAt = LocalDateTime.now();
        }
    }
}
