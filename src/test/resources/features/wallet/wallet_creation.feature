#language: pt
Feature: Criação de Carteira e Consulta de Saldo
  Como usuário do sistema
  Quero criar minha carteira e consultar meu saldo
  Para que eu possa utilizar os serviços financeiros da plataforma

  Background:
    Given que o sistema está disponível

  Scenario: W-01 - Criar carteira com sucesso persiste no banco
    When eu crio uma carteira para o usuário "user-w01"
    Then a resposta deve conter userId "user-w01"
    And a resposta deve conter balance "0.0"
    And deve existir 1 carteira no banco para o usuário "user-w01"

  Scenario: W-02 - Criar carteira duplicada retorna conflito
    Given que existe uma carteira para o usuário "user-w02"
    When eu crio uma carteira para o usuário "user-w02"
    Then a resposta deve retornar status 4xx
    And deve existir apenas 1 carteira no banco para o usuário "user-w02"

  Scenario: W-03 - Criar carteira com userId nulo retorna 400
    When eu crio uma carteira com userId nulo
    Then a resposta deve retornar status 400

  Scenario: W-04 - Criar carteira com userId em branco retorna 400
    When eu crio uma carteira para o usuário "   "
    Then a resposta deve retornar status 400

  Scenario: W-05 - Consultar saldo de carteira recém-criada retorna zero
    Given que existe uma carteira para o usuário "user-w05"
    When eu consulto o saldo do usuário "user-w05"
    Then a resposta deve conter balance "0.0"
    And a resposta deve conter userId "user-w05"

  Scenario: W-06 - Consultar saldo de carteira inexistente retorna 404
    When eu consulto o saldo do usuário "ghost-user-does-not-exist"
    Then a resposta deve retornar status 4xx
