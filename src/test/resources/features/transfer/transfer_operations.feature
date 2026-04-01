Feature: Transfer Operations

  Background:
    Given the system is available

  Scenario: T-01 - Successful transfer updates both wallets atomically
    Given a wallet exists for user "user-t01-a"
    And a wallet exists for user "user-t01-b"
    And user "user-t01-a" has a balance of "100.00"
    When I transfer "50.00" from "user-t01-a" to "user-t01-b"
    Then there should be 2 transactions linked to the transfer within 10 seconds
    And the balance of "user-t01-a" should be "50.00"
    And the balance of "user-t01-b" should be "50.00"

  Scenario: T-02 - Transfer with insufficient funds does not change any wallet
    Given a wallet exists for user "user-t02-a"
    And a wallet exists for user "user-t02-b"
    And user "user-t02-a" has a balance of "100.00"
    When I transfer "200.00" from "user-t02-a" to "user-t02-b"
    Then after waiting 3 seconds there should be no transfers in the database
    And the balance of "user-t02-a" should be "100.00"
    And the balance of "user-t02-b" should be "0.00"

  Scenario: T-03 - Transfer from non-existent source wallet persists nothing
    Given a wallet exists for user "user-t03-b"
    When I transfer "50.00" from "ghost-user-t03" to "user-t03-b"
    Then after waiting 3 seconds there should be no transfers in the database
    And the balance of "user-t03-b" should be "0.00"

  Scenario: T-04 - Transfer to non-existent destination wallet does not change source
    Given a wallet exists for user "user-t04-a"
    And user "user-t04-a" has a balance of "100.00"
    When I transfer "50.00" from "user-t04-a" to "ghost-destination-t04"
    Then after waiting 3 seconds there should be no transfers in the database
    And the balance of "user-t04-a" should be "100.00"

  Scenario: T-05 - Transfer to self results in zero linked transactions
    Given a wallet exists for user "user-t05"
    And user "user-t05" has a balance of "100.00"
    When I transfer "50.00" from "user-t05" to "user-t05"
    Then there should be no transactions linked to the transfer

  Scenario: T-06 - Transfer with zero amount returns 400
    Given a wallet exists for user "user-t06-a"
    And a wallet exists for user "user-t06-b"
    When I transfer "0.00" from "user-t06-a" to "user-t06-b"
    Then the response should return status 400

  Scenario: T-07 - Transfer record correctly links debit and credit transactions
    Given a wallet exists for user "user-t07-a"
    And a wallet exists for user "user-t07-b"
    And user "user-t07-a" has a balance of "100.00"
    When I transfer "30.00" from "user-t07-a" to "user-t07-b"
    Then there should be 1 transfer record in the database within 10 seconds
    And the transfer record should have distinct debit_transaction_id and credit_transaction_id

  Scenario: T-08 - Transfer exact balance zeroes source and credits destination correctly
    Given a wallet exists for user "user-t08-a"
    And a wallet exists for user "user-t08-b"
    And user "user-t08-a" has a balance of "60.00"
    And user "user-t08-b" has a balance of "20.00"
    When I transfer "60.00" from "user-t08-a" to "user-t08-b"
    Then there should be 1 transfer record in the database within 10 seconds
    And the balance of "user-t08-a" should be "0.00"
    And the balance of "user-t08-b" should be "80.00"