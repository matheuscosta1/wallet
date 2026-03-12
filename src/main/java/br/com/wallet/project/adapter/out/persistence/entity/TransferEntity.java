package br.com.wallet.project.adapter.out.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity
@Builder @Getter
@NoArgsConstructor @AllArgsConstructor
@Table(name = "transfers") //TODO: grava as transferencias -- será feito por último essa etapa
public class TransferEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "from_wallet_id", nullable = false)
    private WalletEntity fromWalletEntity;
    @ManyToOne
    @JoinColumn(name = "to_wallet_id", nullable = false)
    private WalletEntity toWalletEntity;
    @Column(nullable = false)
    private BigDecimal amount;
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
    @OneToOne
    @JoinColumn(name = "debit_transaction_id", nullable = false)
    private TransactionEntity debitTransactionEntity;
    @OneToOne
    @JoinColumn(name = "credit_transaction_id", nullable = false)
    private TransactionEntity creditTransactionEntity;
}
