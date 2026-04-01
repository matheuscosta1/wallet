#language: pt
Feature: Operações de Saque
  Como usuário do sistema de carteira
  Quero realizar saques da minha carteira
  Para que meu saldo seja debitado corretamente

  Background:
    Given que o sistema está disponível

  Scenario: WD-01 - Saque dentro do saldo atualiza balance corretamente
    Given que existe uma carteira para o usuário "user-wd01"
    And o usuário "user-wd01" tem saldo de "100.00"
    When eu realizo um saque de "40.00" para o usuário "user-wd01"
    Then deve existir 1 transação do tipo "WITHDRAW" para o usuário "user-wd01" em até 10 segundos
    And o saldo da carteira de "user-wd01" deve ser "60.00"

  Scenario: WD-02 - Saque do saldo exato deixa carteira zerada
    Given que existe uma carteira para o usuário "user-wd02"
    And o usuário "user-wd02" tem saldo de "75.00"
    When eu realizo um saque de "75.00" para o usuário "user-wd02"
    Then deve existir 1 transação do tipo "WITHDRAW" para o usuário "user-wd02" em até 10 segundos
    And o saldo da carteira de "user-wd02" deve ser "0.00"

  Scenario: WD-03 - Saque maior que saldo é rejeitado e saldo permanece inalterado
    Given que existe uma carteira para o usuário "user-wd03"
    And o usuário "user-wd03" tem saldo de "100.00"
    When eu realizo um saque de "200.00" para o usuário "user-wd03"
    Then a resposta deve retornar status 200
    And após aguardar 3 segundos não deve existir nenhuma transação do tipo "WITHDRAW" para o usuário "user-wd03"
    And o saldo da carteira de "user-wd03" deve ser "100.00"

  Scenario: WD-04 - Saque em carteira vazia é rejeitado
    Given que existe uma carteira para o usuário "user-wd04"
    When eu realizo um saque de "10.00" para o usuário "user-wd04"
    Then após aguardar 3 segundos não deve existir nenhuma transação do tipo "WITHDRAW" para o usuário "user-wd04"
    And o saldo da carteira de "user-wd04" deve ser "0.00"

  Scenario: WD-05 - Saque com valor zero é rejeitado
    Given que existe uma carteira para o usuário "user-wd05"
    When eu realizo um saque de "0.00" para o usuário "user-wd05"
    Then a resposta deve retornar status 400

  Scenario: WD-06 - Saque com valor negativo é rejeitado
    Given que existe uma carteira para o usuário "user-wd06"
    When eu realizo um saque de "-30.00" para o usuário "user-wd06"
    Then a resposta deve retornar status 400

  Scenario: WD-07 - Registro de transação salva saldo antes e depois do saque
    Given que existe uma carteira para o usuário "user-wd07"
    And o usuário "user-wd07" tem saldo de "100.00"
    When eu realizo um saque de "30.00" para o usuário "user-wd07"
    Then deve existir 1 transação do tipo "WITHDRAW" para o usuário "user-wd07" em até 10 segundos
    And o saldo antes da transação de saque de "user-wd07" deve ser "100.00"
    And o saldo depois da transação de saque de "user-wd07" deve ser "70.00"
