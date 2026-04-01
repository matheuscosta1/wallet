Feature: Idempotency - Redis-based Deduplication

  Background:
    Given the system is available

  Scenario: I-01 - Same idempotencyId sent twice sequentially processes only once
    Given a wallet exists for user "user-i01"
    When I make 2 deposits of "100.00" for user "user-i01" with the same idempotencyId
    Then there should be 1 transaction for user "user-i01" within 10 seconds
    And the balance of "user-i01" should be "100.00"

  Scenario: I-02 - Same idempotencyId sent 5 times processes exactly once
    Given a wallet exists for user "user-i02"
    When I make 5 deposits of "50.00" for user "user-i02" with the same idempotencyId
    Then there should be 1 transaction for user "user-i02" within 10 seconds
    And the balance of "user-i02" should be "50.00"

  Scenario: I-03 - Two deposits with different idempotencyIds are processed independently
    Given a wallet exists for user "user-i03"
    When I make 2 deposits of "100.00" for user "user-i03" with different idempotencyIds
    Then there should be 2 transactions for user "user-i03" within 10 seconds
    And the balance of "user-i03" should be "200.00"

  Scenario: I-04 - Same idempotencyId fired by 10 concurrent threads processes only once
    Given a wallet exists for user "user-i04"
    When 10 threads concurrently send the same deposit of "100.00" for "user-i04"
    Then there should be 1 transaction for user "user-i04" within 10 seconds
    And the balance of "user-i04" should be "100.00"

  Scenario: I-05 - Same idempotencyId for transfer sent twice processes only once
    Given a wallet exists for user "user-i05-a"
    And a wallet exists for user "user-i05-b"
    And user "user-i05-a" has a balance of "100.00"
    When I make 2 transfers of "40.00" from "user-i05-a" to "user-i05-b" with the same idempotencyId
    Then there should be 1 transfer record in the database within 10 seconds
    And the balance of "user-i05-a" should be "60.00"
    And the balance of "user-i05-b" should be "40.00"

  Scenario: I-06 - Same idempotencyId for withdraw sent twice processes only once
    Given a wallet exists for user "user-i06"
    And user "user-i06" has a balance of "100.00"
    When I make 2 withdraws of "30.00" for user "user-i06" with the same idempotencyId
    Then there should be 1 transaction of type "WITHDRAW" for user "user-i06" within 10 seconds
    And the balance of "user-i06" should be "70.00"

  Scenario: I-07 - After Redis key expires the same idempotencyId is treated as new
    Given a wallet exists for user "user-i07"
    When I deposit "50.00" for user "user-i07" with fixed idempotencyId "6a2ff239-6936-41e8-9f1a-9caecd1fd0ca"
    Then there should be 1 transaction for user "user-i07" within 10 seconds
    When all Redis keys are deleted
    And I deposit "50.00" for user "user-i07" with fixed idempotencyId "6a2ff239-6936-41e8-9f1a-9caecd1fd0ca"
    Then there should be 2 transactions for user "user-i07" within 10 seconds
    And the balance of "user-i07" should be "100.00"

  Scenario: I-08 - Null idempotencyId in deposit returns 400
    Given a wallet exists for user "user-i08"
    When I deposit "100.00" for user "user-i08" with null idempotencyId
    Then the response should return status 400

  Scenario: I-09 - Blank idempotencyId in deposit returns 400
    Given a wallet exists for user "user-i09"
    When I deposit "100.00" for user "user-i09" with idempotencyId "   "
    Then the response should return status 400