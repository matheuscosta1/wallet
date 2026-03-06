package br.com.wallet.project.shared.mapper;
import br.com.wallet.project.adapter.out.persistence.entity.WalletEntity;
import br.com.wallet.project.domain.model.Wallet;
public class WalletEntityMapper {
    private WalletEntityMapper() {}
    public static Wallet toDomain(WalletEntity entity) {
        if (entity == null) return null;
        return Wallet.builder().id(entity.getId()).userId(entity.getUserId()).balance(entity.getBalance()).version(entity.getVersion()).build();
    }
    public static WalletEntity toEntity(Wallet wallet) {
        return WalletEntity.builder().id(wallet.getId()).userId(wallet.getUserId()).balance(wallet.getBalance()).version(wallet.getVersion()).build();
    }
}
