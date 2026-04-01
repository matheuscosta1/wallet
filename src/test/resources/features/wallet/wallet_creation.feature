Feature: Wallet Creation and Balance Retrieval

  Background:
    Given the system is available

  Scenario: W-01 - Successfully create wallet and persist in database
    When I create a wallet for user "user-w01"
    Then the response should contain userId "user-w01"
    And the response should contain balance "0.0"
    And there should be 1 wallet in the database for user "user-w01"

  Scenario: W-02 - Creating duplicate wallet returns conflict
    Given a wallet exists for user "user-w02"
    When I create a wallet for user "user-w02"
    Then the response should return status 4xx
    And there should be only 1 wallet in the database for user "user-w02"

  Scenario: W-03 - Creating wallet with null userId returns 400
    When I create a wallet with null userId
    Then the response should return status 400

  Scenario: W-04 - Creating wallet with blank userId returns 400
    When I create a wallet for user "   "
    Then the response should return status 400

  Scenario: W-05 - Retrieve balance of newly created wallet returns zero
    Given a wallet exists for user "user-w05"
    When I retrieve the balance for user "user-w05"
    Then the response should contain balance "0.0"
    And the response should contain userId "user-w05"

  Scenario: W-06 - Retrieve balance of non-existent wallet returns 4xx
    When I retrieve the balance for user "ghost-user-does-not-exist"
    Then the response should return status 4xx