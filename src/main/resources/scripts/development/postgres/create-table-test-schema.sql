CREATE TABLE wallets (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL UNIQUE,
    balance DECIMAL(19, 4) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE transactions (
    id SERIAL PRIMARY KEY,
    wallet_id INT NOT NULL,
    transaction_track_id UUID NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    type VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    balance_before_transaction DECIMAL(19, 4) NOT NULL,
    balance_after_transaction DECIMAL(19, 4) NOT NULL,
    CONSTRAINT fk_transactions_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE CASCADE
);
CREATE TABLE transfers (
    id SERIAL PRIMARY KEY,
    from_wallet_id INT NOT NULL,
    to_wallet_id INT NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    debit_transaction_id INT NOT NULL UNIQUE,
    credit_transaction_id INT NOT NULL UNIQUE,
    CONSTRAINT fk_transfers_from_wallet FOREIGN KEY (from_wallet_id) REFERENCES wallets(id) ON DELETE CASCADE,
    CONSTRAINT fk_transfers_to_wallet FOREIGN KEY (to_wallet_id) REFERENCES wallets(id) ON DELETE CASCADE,
    CONSTRAINT fk_transfers_debit_transaction FOREIGN KEY (debit_transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
    CONSTRAINT fk_transfers_credit_transaction FOREIGN KEY (credit_transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
);

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



