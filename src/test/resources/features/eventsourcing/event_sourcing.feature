#language: pt
Feature: Event Sourcing - Log de Eventos de Domínio
  Como sistema de carteira
  Quero registrar todos os eventos de domínio no log de eventos
  Para garantir rastreabilidade e auditoria completa das operações

  Background:
    Given que o sistema está disponível

  Scenario: ES-01 - Criação de carteira emite exatamente um evento WALLET_CREATED
    When eu crio uma carteira para o usuário "user-es01"
    Then deve existir exatamente 1 evento do tipo "WALLET_CREATED" para o agregado "user-es01"

  Scenario: ES-02 - Depósito emite DEPOSIT_REQUESTED sincrono e DEPOSIT_COMPLETED assíncrono
    Given que existe uma carteira para o usuário "user-es02"
    When eu realizo um depósito de "100.00" para o usuário "user-es02"
    Then deve existir exatamente 1 evento do tipo "DEPOSIT_REQUESTED" para o agregado "user-es02"
    And deve existir exatamente 1 evento do tipo "DEPOSIT_COMPLETED" para o agregado "user-es02" em até 10 segundos
    And não deve existir nenhum evento do tipo "TRANSACTION_RETRY_ATTEMPTED" para o agregado "user-es02"

  Scenario: ES-03 - Saque bem-sucedido emite WITHDRAW_REQUESTED e WITHDRAW_COMPLETED sem retentativas
    Given que existe uma carteira para o usuário "user-es03"
    And o usuário "user-es03" tem saldo de "100.00"
    When eu realizo um saque de "40.00" para o usuário "user-es03"
    Then deve existir exatamente 1 evento do tipo "WITHDRAW_COMPLETED" para o agregado "user-es03" em até 10 segundos
    And não deve existir nenhum evento do tipo "TRANSACTION_RETRY_ATTEMPTED" para o agregado "user-es03"

  Scenario: ES-04 - Transferência bem-sucedida emite cadeia completa de 4 eventos sem retentativas
    Given que existe uma carteira para o usuário "user-es04-a"
    And que existe uma carteira para o usuário "user-es04-b"
    And o usuário "user-es04-a" tem saldo de "100.00"
    When eu transfiro "30.00" de "user-es04-a" para "user-es04-b" e capturo o transactionId
    Then deve existir exatamente 1 evento do tipo "TRANSFER_REQUESTED" para o agregado de transferência em até 10 segundos
    And deve existir exatamente 1 evento do tipo "TRANSFER_COMPLETED" para o agregado de transferência
    And deve existir exatamente 1 evento do tipo "WITHDRAW_COMPLETED" para o agregado "user-es04-a"
    And deve existir exatamente 1 evento do tipo "DEPOSIT_COMPLETED" para o agregado "user-es04-b"
    And não deve existir nenhum evento de retry para o agregado de transferência

  Scenario: ES-05 - Saldo insuficiente gera WITHDRAW_REQUESTED + retentativas + WITHDRAW_PERMANENTLY_FAILED
    Given que existe uma carteira para o usuário "user-es05"
    And o usuário "user-es05" tem saldo de "50.00"
    When eu realizo um saque de "200.00" para o usuário "user-es05"
    Then deve existir exatamente 1 evento do tipo "WITHDRAW_REQUESTED" para o agregado "user-es05"
    And deve existir exatamente 1 evento do tipo "WITHDRAW_PERMANENTLY_FAILED" para o agregado "user-es05" em até 25 segundos
    And não deve existir nenhum evento do tipo "WITHDRAW_COMPLETED" para o agregado "user-es05"
    And não deve existir nenhum evento do tipo "WITHDRAW_FAILED" para o agregado "user-es05"

  Scenario: ES-06 - Depósito em carteira inexistente gera DEPOSIT_REQUESTED + retentativas + DEPOSIT_PERMANENTLY_FAILED
    When eu realizo um depósito de "100.00" para o usuário "ghost-es06"
    Then deve existir exatamente 1 evento do tipo "DEPOSIT_REQUESTED" para o agregado "ghost-es06"
    And deve existir exatamente 1 evento do tipo "DEPOSIT_PERMANENTLY_FAILED" para o agregado "ghost-es06" em até 25 segundos
    And não deve existir nenhum evento do tipo "DEPOSIT_COMPLETED" para o agregado "ghost-es06"
    And não deve existir nenhum evento do tipo "DEPOSIT_FAILED" para o agregado "ghost-es06"

  Scenario: ES-07 - idempotencyId duplicado gera DUPLICATE_DETECTED + retentativas + DEPOSIT_PERMANENTLY_FAILED
    Given que existe uma carteira para o usuário "user-es07"
    And eu realizo um depósito de "100.00" para o usuário "user-es07" e aguardo o processamento
    When eu realizo novamente o mesmo depósito com o mesmo idempotencyId para "user-es07"
    Then deve existir exatamente 1 evento do tipo "DEPOSIT_PERMANENTLY_FAILED" para o agregado "user-es07" em até 25 segundos
    And deve existir exatamente 1 evento do tipo "DEPOSIT_COMPLETED" para o agregado "user-es07"
    And o saldo da carteira de "user-es07" deve ser "100.00"

  Scenario: ES-08 - Payload de DEPOSIT_COMPLETED contém snapshots corretos de saldo
    Given que existe uma carteira para o usuário "user-es08"
    And o usuário "user-es08" tem saldo de "200.00"
    Then o payload do evento "DEPOSIT_COMPLETED" de "user-es08" deve conter balanceBefore igual a "0.00"
    And o payload do evento "DEPOSIT_COMPLETED" de "user-es08" deve conter balanceAfter igual a "200.00"

  Scenario: ES-09 - Payload de TRANSFER_COMPLETED contém os 4 snapshots de saldo
    Given que existe uma carteira para o usuário "user-es09-a"
    And que existe uma carteira para o usuário "user-es09-b"
    And o usuário "user-es09-a" tem saldo de "100.00"
    When eu transfiro "40.00" de "user-es09-a" para "user-es09-b" e capturo o transactionId
    Then o payload do evento "TRANSFER_COMPLETED" do agregado de transferência deve conter:
      | campo            | valor  |
      | fromBalanceBefore | 100.00 |
      | fromBalanceAfter  | 60.00  |
      | toBalanceBefore   | 0.00   |
      | toBalanceAfter    | 40.00  |

  Scenario: ES-10 - Payload de WITHDRAW_PERMANENTLY_FAILED contém reason, retryCount e failedEventType
    Given que existe uma carteira para o usuário "user-es10"
    And o usuário "user-es10" tem saldo de "50.00"
    When eu realizo um saque de "999.00" para o usuário "user-es10"
    Then deve existir exatamente 1 evento do tipo "WITHDRAW_PERMANENTLY_FAILED" para o agregado "user-es10" em até 25 segundos
    And o payload do evento "WITHDRAW_PERMANENTLY_FAILED" de "user-es10" deve conter reason com "Insufficient funds to withdraw."
    And o payload do evento "WITHDRAW_PERMANENTLY_FAILED" de "user-es10" deve conter retryCount igual a "4"

  Scenario: ES-11 - Eventos *_FAILED nunca aparecem como linhas independentes no log
    Given que existe uma carteira para o usuário "user-es11"
    And o usuário "user-es11" tem saldo de "50.00"
    When eu realizo um saque de "200.00" para o usuário "user-es11"
    Then deve existir exatamente 1 evento do tipo "WITHDRAW_PERMANENTLY_FAILED" para o agregado "user-es11" em até 25 segundos
    And não deve existir nenhum evento dos tipos "DEPOSIT_FAILED", "WITHDRAW_FAILED" ou "TRANSFER_FAILED" para "user-es11"

  Scenario: ES-12 - create + deposit + withdraw gera exatamente 5 eventos no log
    Given que existe uma carteira para o usuário "user-es12"
    And o usuário "user-es12" tem saldo de "100.00"
    When eu realizo um saque de "30.00" para o usuário "user-es12"
    Then deve existir exatamente 1 evento do tipo "WITHDRAW_COMPLETED" para o agregado "user-es12" em até 10 segundos
    And o total de eventos para o agregado "user-es12" deve ser exatamente 5

  Scenario: ES-13 - Payload de WITHDRAW_PERMANENTLY_FAILED contém transactionType e reason
    Given que existe uma carteira para o usuário "user-es13"
    And o usuário "user-es13" tem saldo de "50.00"
    When eu realizo um saque de "999.00" para o usuário "user-es13"
    Then deve existir exatamente 1 evento do tipo "WITHDRAW_PERMANENTLY_FAILED" para o agregado "user-es13" em até 25 segundos
    And o payload do evento "WITHDRAW_PERMANENTLY_FAILED" de "user-es13" deve conter transactionType igual a "WITHDRAW"
    And o payload do evento "WITHDRAW_PERMANENTLY_FAILED" de "user-es13" deve conter reason com "Insufficient funds to withdraw."
