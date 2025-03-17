package br.com.wallet.project.domain.enums;

import lombok.AllArgsConstructor;

import java.text.MessageFormat;

@AllArgsConstructor
public enum WalletErrors {
  W0001("Wallet already created with this user id {0}.", 1000),
  W0002("Can not retrieve balance for user id {0} because wallet does not exist.", 1000),
  W0003("Transaction of type {0} can not be completed.", 1000),
  W0004("Insufficient funds to withdraw.", 1000),
  W0005("Unsupported transaction type: {0}.", 1000),
  W0006("Wallet does not exist for user id {0}.", 1000),
  W0007("Deposit amount must be greater than zero. User id: {0}, transaction id {1}", 1000),
  W0008("Duplicate transaction detected for transaction id: {0}", 1000);

  private final String message;
  private final int group;

  public String message() {
    return message;
  }

  public int group() {
    return group;
  }
}
