Feature: Concurrency Control - Pessimistic Lock and Version

  Background:
    Given the system is available

  Scenario: C-01 - 10 concurrent deposits of 10.00 result in exact balance of 100.00
    Given a wallet exists for user "user-c01"
    When 10 threads concurrently send deposits of "10.00" for "user-c01"
    Then there should be 10 transactions of type "DEPOSIT" for user "user-c01" within 30 seconds
    And the balance of "user-c01" should be "100.00"

  Scenario: C-02 - 10 concurrent withdraws of 10.00 from 100.00 result in 0.00 without overdraft
    Given a wallet exists for user "user-c02"
    And user "user-c02" has a balance of "100.00"
    When 10 threads concurrently send withdraws of "10.00" for "user-c02"
    Then there should be 10 transactions of type "WITHDRAW" for user "user-c02" within 30 seconds
    And the balance of "user-c02" should be "0.00"
    And the balance of "user-c02" should be greater than or equal to "0.00"

  Scenario: C-03 - 5 concurrent deposits and 5 withdraws result in consistent balance of 150.00
    Given a wallet exists for user "user-c03"
    And user "user-c03" has a balance of "100.00"
    When 5 threads send deposits of "20.00" and 5 threads send withdraws of "10.00" concurrently for "user-c03"
    Then there should be 11 transactions for user "user-c03" within 30 seconds
    And the balance of "user-c03" should be "150.00"

  Scenario: C-04 - 5 concurrent transfers from A to B preserve total sum
    Given a wallet exists for user "user-c04-a"
    And a wallet exists for user "user-c04-b"
    And user "user-c04-a" has a balance of "100.00"
    When 5 threads concurrently send transfers of "10.00" from "user-c04-a" to "user-c04-b"
    Then there should be 5 transfer records in the database within 30 seconds
    And the sum of balances of "user-c04-a" and "user-c04-b" should be "100.00"
    And the balance of "user-c04-a" should be "50.00"
    And the balance of "user-c04-b" should be "50.00"

  Scenario: C-05 - 5 A to B and 5 B to A concurrent transfers without deadlock preserve total
    Given a wallet exists for user "user-c05-a"
    And a wallet exists for user "user-c05-b"
    And user "user-c05-a" has a balance of "50.00"
    And user "user-c05-b" has a balance of "50.00"
    When 5 threads transfer "5.00" from "user-c05-a" to "user-c05-b" and 5 threads transfer "5.00" from "user-c05-b" to "user-c05-a" concurrently
    Then there should be 10 transfer records in the database within 30 seconds
    And the sum of balances of "user-c05-a" and "user-c05-b" should be "100.00"
    And the balance of "user-c05-a" should be greater than or equal to "0.00"
    And the balance of "user-c05-b" should be greater than or equal to "0.00"

  Scenario: C-06 - Stress test 100 concurrent deposits of 1.00 result in exact balance of 100.00
    Given a wallet exists for user "user-c06"
    When 100 threads concurrently send deposits of "1.00" for "user-c06"
    Then there should be 100 transactions of type "DEPOSIT" for user "user-c06" within 30 seconds
    And the balance of "user-c06" should be "100.00"