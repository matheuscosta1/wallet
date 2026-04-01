#language: pt
Feature: Controle de Concorrência - Pessimistic Lock e @Version
  Como sistema de carteira
  Quero garantir que operações concorrentes não gerem inconsistências de saldo
  Para que o sistema seja financeiramente confiável

  Background:
    Given que o sistema está disponível

  Scenario: C-01 - 10 depósitos concorrentes de 10.00 resultam em saldo exato de 100.00
    Given que existe uma carteira para o usuário "user-c01"
    When 10 threads enviam simultaneamente depósitos de "10.00" para "user-c01"
    Then deve existir 10 transações do tipo "DEPOSIT" para o usuário "user-c01" em até 30 segundos
    And o saldo da carteira de "user-c01" deve ser "100.00"

  Scenario: C-02 - 10 saques concorrentes de 10.00 a partir de 100.00 resultam em saldo 0.00 sem overdraft
    Given que existe uma carteira para o usuário "user-c02"
    And o usuário "user-c02" tem saldo de "100.00"
    When 10 threads enviam simultaneamente saques de "10.00" para "user-c02"
    Then deve existir 10 transações do tipo "WITHDRAW" para o usuário "user-c02" em até 30 segundos
    And o saldo da carteira de "user-c02" deve ser "0.00"
    And o saldo da carteira de "user-c02" deve ser maior ou igual a "0.00"

  Scenario: C-03 - 5 depósitos e 5 saques concorrentes resultam em saldo consistente de 150.00
    Given que existe uma carteira para o usuário "user-c03"
    And o usuário "user-c03" tem saldo de "100.00"
    When 5 threads enviam depósitos de "20.00" e 5 threads enviam saques de "10.00" concorrentemente para "user-c03"
    Then deve existir 11 transações para o usuário "user-c03" em até 30 segundos
    And o saldo da carteira de "user-c03" deve ser "150.00"

  Scenario: C-04 - 5 transferências concorrentes de A para B preservam a soma total
    Given que existe uma carteira para o usuário "user-c04-a"
    And que existe uma carteira para o usuário "user-c04-b"
    And o usuário "user-c04-a" tem saldo de "100.00"
    When 5 threads enviam simultaneamente transferências de "10.00" de "user-c04-a" para "user-c04-b"
    Then deve existir 5 registros de transferência no banco em até 30 segundos
    And a soma dos saldos de "user-c04-a" e "user-c04-b" deve ser "100.00"
    And o saldo da carteira de "user-c04-a" deve ser "50.00"
    And o saldo da carteira de "user-c04-b" deve ser "50.00"

  Scenario: C-05 - 5 transferências A->B e 5 B->A concorrentes sem deadlock preservam a soma
    Given que existe uma carteira para o usuário "user-c05-a"
    And que existe uma carteira para o usuário "user-c05-b"
    And o usuário "user-c05-a" tem saldo de "50.00"
    And o usuário "user-c05-b" tem saldo de "50.00"
    When 5 threads transferem "5.00" de "user-c05-a" para "user-c05-b" e 5 threads transferem "5.00" de "user-c05-b" para "user-c05-a" concorrentemente
    Then deve existir 10 registros de transferência no banco em até 30 segundos
    And a soma dos saldos de "user-c05-a" e "user-c05-b" deve ser "100.00"
    And o saldo da carteira de "user-c05-a" deve ser maior ou igual a "0.00"
    And o saldo da carteira de "user-c05-b" deve ser maior ou igual a "0.00"

  Scenario: C-06 - Stress test: 100 depósitos concorrentes de 1.00 resultam em saldo exato de 100.00
    Given que existe uma carteira para o usuário "user-c06"
    When 100 threads enviam simultaneamente depósitos de "1.00" para "user-c06"
    Then deve existir 100 transações do tipo "DEPOSIT" para o usuário "user-c06" em até 30 segundos
    And o saldo da carteira de "user-c06" deve ser "100.00"
