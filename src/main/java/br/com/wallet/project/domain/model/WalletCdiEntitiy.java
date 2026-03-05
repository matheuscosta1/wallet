package br.com.wallet.project.domain.model;

import br.com.wallet.project.domain.enums.WalletErrors;
import br.com.wallet.project.exception.WalletException;
import br.com.wallet.project.util.MoneyUtil;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@Table(name = "cdi")
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class WalletCdiEntitiy {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userId;

    @Column(nullable = false, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, scale = 2)
    private BigDecimal CdiValue;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}
