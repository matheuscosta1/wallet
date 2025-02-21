package br.com.wallet.project.repositoy;

import br.com.wallet.project.repositoy.model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
}
