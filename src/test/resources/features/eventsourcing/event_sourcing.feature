Feature: Event Sourcing - Domain Event Log

  Background:
    Given the system is available

  Scenario: ES-01 - Creating a wallet emits exactly one WALLET_CREATED event
    When I create a wallet for user "user-es01"
    Then there should be exactly 1 event of type "WALLET_CREATED" for aggregate "user-es01"

  Scenario: ES-02 - Deposit emits DEPOSIT_REQUESTED sync and DEPOSIT_COMPLETED async
    Given a wallet exists for user "user-es02"
    When I deposit "100.00" for user "user-es02"
    Then there should be exactly 1 event of type "DEPOSIT_REQUESTED" for aggregate "user-es02"
    And there should be exactly 1 event of type "DEPOSIT_COMPLETED" for aggregate "user-es02" within 10 seconds
    And there should be no event of type "TRANSACTION_RETRY_ATTEMPTED" for aggregate "user-es02"

  Scenario: ES-03 - Successful withdraw emits WITHDRAW_REQUESTED and WITHDRAW_COMPLETED without retries
    Given a wallet exists for user "user-es03"
    And user "user-es03" has a balance of "100.00"
    When I withdraw "40.00" for user "user-es03"
    Then there should be exactly 1 event of type "WITHDRAW_COMPLETED" for aggregate "user-es03" within 10 seconds
    And there should be no event of type "TRANSACTION_RETRY_ATTEMPTED" for aggregate "user-es03"

  Scenario: ES-04 - Successful transfer emits full 4-event chain without retries
    Given a wallet exists for user "user-es04-a"
    And a wallet exists for user "user-es04-b"
    And user "user-es04-a" has a balance of "100.00"
    When I transfer "30.00" from "user-es04-a" to "user-es04-b" and capture the transactionId
    Then there should be exactly 1 event of type "TRANSFER_REQUESTED" for the transfer aggregate within 10 seconds
    And there should be exactly 1 event of type "TRANSFER_COMPLETED" for the transfer aggregate
    And there should be exactly 1 event of type "WITHDRAW_COMPLETED" for aggregate "user-es04-a"
    And there should be exactly 1 event of type "DEPOSIT_COMPLETED" for aggregate "user-es04-b"
    And there should be no retry events for the transfer aggregate

  Scenario: ES-05 - Insufficient funds generates WITHDRAW_REQUESTED plus retries plus WITHDRAW_PERMANENTLY_FAILED
    Given a wallet exists for user "user-es05"
    And user "user-es05" has a balance of "50.00"
    When I withdraw "200.00" for user "user-es05"
    Then there should be exactly 1 event of type "WITHDRAW_REQUESTED" for aggregate "user-es05"
    And there should be exactly 1 event of type "WITHDRAW_PERMANENTLY_FAILED" for aggregate "user-es05" within 25 seconds
    And there should be no event of type "WITHDRAW_COMPLETED" for aggregate "user-es05"
    And there should be no event of type "WITHDRAW_FAILED" for aggregate "user-es05"

  Scenario: ES-06 - Deposit to non-existent wallet generates DEPOSIT_REQUESTED plus retries plus DEPOSIT_PERMANENTLY_FAILED
    When I deposit "100.00" for user "ghost-es06"
    Then there should be exactly 1 event of type "DEPOSIT_REQUESTED" for aggregate "ghost-es06"
    And there should be exactly 1 event of type "DEPOSIT_PERMANENTLY_FAILED" for aggregate "ghost-es06" within 25 seconds
    And there should be no event of type "DEPOSIT_COMPLETED" for aggregate "ghost-es06"
    And there should be no event of type "DEPOSIT_FAILED" for aggregate "ghost-es06"

  Scenario: ES-07 - Duplicate idempotencyId generates DUPLICATE_DETECTED plus retries plus DEPOSIT_PERMANENTLY_FAILED
    Given a wallet exists for user "user-es07"
    And I deposit "100.00" for user "user-es07" and wait for processing
    When I send the same deposit again with the same idempotencyId for "user-es07"
    Then there should be exactly 1 event of type "DEPOSIT_PERMANENTLY_FAILED" for aggregate "user-es07" within 25 seconds
    And there should be exactly 1 event of type "DEPOSIT_COMPLETED" for aggregate "user-es07"
    And the balance of "user-es07" should be "100.00"

  Scenario: ES-08 - DEPOSIT_COMPLETED payload contains correct balance snapshots
    Given a wallet exists for user "user-es08"
    And user "user-es08" has a balance of "200.00"
    Then the payload of event "DEPOSIT_COMPLETED" for "user-es08" should have balanceBefore equal to "0.00"
    And the payload of event "DEPOSIT_COMPLETED" for "user-es08" should have balanceAfter equal to "200.00"

  Scenario: ES-09 - TRANSFER_COMPLETED payload contains all 4 balance snapshots
    Given a wallet exists for user "user-es09-a"
    And a wallet exists for user "user-es09-b"
    And user "user-es09-a" has a balance of "100.00"
    When I transfer "40.00" from "user-es09-a" to "user-es09-b" and capture the transactionId
    Then the payload of event "TRANSFER_COMPLETED" for the transfer aggregate should contain:
      | field             | value  |
      | fromBalanceBefore | 100.00 |
      | fromBalanceAfter  | 60.00  |
      | toBalanceBefore   | 0.00   |
      | toBalanceAfter    | 40.00  |

  Scenario: ES-10 - WITHDRAW_PERMANENTLY_FAILED payload contains reason retryCount and failedEventType
    Given a wallet exists for user "user-es10"
    And user "user-es10" has a balance of "50.00"
    When I withdraw "999.00" for user "user-es10"
    Then there should be exactly 1 event of type "WITHDRAW_PERMANENTLY_FAILED" for aggregate "user-es10" within 25 seconds
    And the payload of event "WITHDRAW_PERMANENTLY_FAILED" for "user-es10" should have reason containing "Insufficient funds to withdraw."
    And the payload of event "WITHDRAW_PERMANENTLY_FAILED" for "user-es10" should have retryCount equal to "4"

  Scenario: ES-11 - FAILED events never appear as standalone rows in the event log
    Given a wallet exists for user "user-es11"
    And user "user-es11" has a balance of "50.00"
    When I withdraw "200.00" for user "user-es11"
    Then there should be exactly 1 event of type "WITHDRAW_PERMANENTLY_FAILED" for aggregate "user-es11" within 25 seconds
    And there should be no events of types "DEPOSIT_FAILED", "WITHDRAW_FAILED" or "TRANSFER_FAILED" for "user-es11"

  Scenario: ES-12 - create plus deposit plus withdraw generates exactly 5 events
    Given a wallet exists for user "user-es12"
    And user "user-es12" has a balance of "100.00"
    When I withdraw "30.00" for user "user-es12"
    Then there should be exactly 1 event of type "WITHDRAW_COMPLETED" for aggregate "user-es12" within 10 seconds
    And the total event count for aggregate "user-es12" should be exactly 5

  Scenario: ES-13 - WITHDRAW_PERMANENTLY_FAILED payload contains transactionType and reason
    Given a wallet exists for user "user-es13"
    And user "user-es13" has a balance of "50.00"
    When I withdraw "999.00" for user "user-es13"
    Then there should be exactly 1 event of type "WITHDRAW_PERMANENTLY_FAILED" for aggregate "user-es13" within 25 seconds
    And the payload of event "WITHDRAW_PERMANENTLY_FAILED" for "user-es13" should have transactionType equal to "WITHDRAW"
    And the payload of event "WITHDRAW_PERMANENTLY_FAILED" for "user-es13" should have reason containing "Insufficient funds to withdraw."