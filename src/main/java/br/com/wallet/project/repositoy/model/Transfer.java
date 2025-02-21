package br.com.wallet.project.repositoy.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Builder
@Table(name = "transfers") //TODO: grava as transferencias -- será feito por último essa etapa
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "from_wallet_id", nullable = false)
    private Wallet fromWallet;

    @ManyToOne
    @JoinColumn(name = "to_wallet_id", nullable = false)
    private Wallet toWallet;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    @OneToOne
    @JoinColumn(name = "debit_transaction_id", nullable = false)
    private Transaction debitTransaction;

    @OneToOne
    @JoinColumn(name = "credit_transaction_id", nullable = false)
    private Transaction creditTransaction;
}
