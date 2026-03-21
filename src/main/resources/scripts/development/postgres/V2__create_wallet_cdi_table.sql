-- Tabela para armazenar o histórico de rendimentos CDI por carteira
CREATE TABLE wallet_cdi (
    id                  SERIAL PRIMARY KEY,
    wallet_id           INT NOT NULL,
    balance_before_cdi  DECIMAL(19, 4) NOT NULL,
    balance_after_cdi   DECIMAL(19, 4) NOT NULL,
    cdi_rate            DECIMAL(10, 6) NOT NULL,
    yield_amount        DECIMAL(19, 4) NOT NULL,
    calculated_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_wallet_cdi_wallet
        FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE CASCADE
);

CREATE INDEX idx_wallet_cdi_wallet_id ON wallet_cdi(wallet_id);
CREATE INDEX idx_wallet_cdi_calculated_at ON wallet_cdi(calculated_at);
