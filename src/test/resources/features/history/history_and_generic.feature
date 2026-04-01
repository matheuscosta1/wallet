#language: pt
Feature: Histórico de Transações e Endpoint Genérico
  Como usuário do sistema
  Quero consultar meu histórico de transações e usar o endpoint genérico de operações
  Para ter visibilidade das movimentações na minha carteira

  Background:
    Given que o sistema está disponível

  Scenario: H-01 - Histórico após depósito, saque e transferência retorna 3 transações
    Given que existe uma carteira para o usuário "user-h01-a"
    And que existe uma carteira para o usuário "user-h01-b"
    And o usuário "user-h01-a" tem saldo de "200.00"
    And o usuário "user-h01-a" realizou um saque de "50.00"
    And o usuário "user-h01-a" realizou uma transferência de "30.00" para "user-h01-b"
    When eu consulto o histórico do usuário "user-h01-a" para a data de hoje
    Then a resposta deve conter uma lista com 3 transações

  Scenario: H-02 - Histórico filtrado pela data de hoje retorna apenas transações do dia
    Given que existe uma carteira para o usuário "user-h02"
    And o usuário "user-h02" tem saldo de "100.00"
    When eu consulto o histórico do usuário "user-h02" para a data de hoje
    Then a resposta deve conter uma lista com 1 transação

  Scenario: H-03 - Histórico para usuário inexistente retorna 4xx
    When eu consulto o histórico do usuário "ghost-user-h03" para a data de hoje
    Then a resposta deve retornar status 4xx

  Scenario: H-04 - Histórico para usuário sem transações retorna lista vazia
    Given que existe uma carteira para o usuário "user-h04"
    When eu consulto o histórico do usuário "user-h04" para a data de hoje
    Then a resposta deve conter uma lista vazia

  Scenario: H-05 - Histórico com data futura retorna lista vazia
    Given que existe uma carteira para o usuário "user-h05"
    And o usuário "user-h05" tem saldo de "100.00"
    When eu consulto o histórico do usuário "user-h05" para a data "2099-12-31 00:00:00.000"
    Then a resposta deve conter uma lista vazia

  Scenario: H-06 - Cada transação no histórico contém saldo antes e depois
    Given que existe uma carteira para o usuário "user-h06"
    And o usuário "user-h06" tem saldo de "100.00"
    When eu consulto o histórico do usuário "user-h06" para a data de hoje
    Then a primeira transação deve conter balanceBeforeTransaction igual a "0.0"
    And a primeira transação deve conter balanceAfterTransaction igual a "100.0"

  Scenario: G-01 - Endpoint genérico com tipo DEPOSIT processa corretamente
    Given que existe uma carteira para o usuário "user-g01"
    When eu realizo uma operação genérica do tipo "DEPOSIT" de "80.00" para o usuário "user-g01"
    Then deve existir 1 transação do tipo "DEPOSIT" para o usuário "user-g01" em até 10 segundos
    And o saldo da carteira de "user-g01" deve ser "80.00"

  Scenario: G-02 - Endpoint genérico com tipo WITHDRAW processa corretamente
    Given que existe uma carteira para o usuário "user-g02"
    And o usuário "user-g02" tem saldo de "100.00"
    When eu realizo uma operação genérica do tipo "WITHDRAW" de "25.00" para o usuário "user-g02"
    Then deve existir 1 transação do tipo "WITHDRAW" para o usuário "user-g02" em até 10 segundos
    And o saldo da carteira de "user-g02" deve ser "75.00"

  Scenario: G-03 - Endpoint genérico com tipo TRANSFER processa corretamente
    Given que existe uma carteira para o usuário "user-g03-a"
    And que existe uma carteira para o usuário "user-g03-b"
    And o usuário "user-g03-a" tem saldo de "100.00"
    When eu realizo uma operação genérica de transferência de "35.00" de "user-g03-a" para "user-g03-b"
    Then deve existir 1 registro de transferência no banco em até 10 segundos
    And o saldo da carteira de "user-g03-a" deve ser "65.00"

  Scenario: G-04 - Endpoint genérico com tipo desconhecido retorna 400
    Given que existe uma carteira para o usuário "user-g04"
    When eu realizo uma operação genérica do tipo "INVALID_TYPE" de "50.00" para o usuário "user-g04"
    Then a resposta deve retornar status 400
