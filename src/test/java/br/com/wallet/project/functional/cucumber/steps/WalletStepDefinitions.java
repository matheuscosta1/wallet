package br.com.wallet.project.functional.cucumber.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import br.com.wallet.project.functional.BaseFunctionalTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.java.Before;

public class WalletStepDefinitions {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private RedisTemplate<String, String> redisTemplate;
    @Autowired private ObjectMapper objectMapper;

    private MvcResult lastResult;
    private long requestStartTime;
    private String reusableIdempotencyId;
    private String capturedTransferAggregateId;


    @Before
    public void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM transfers");
        jdbcTemplate.execute("DELETE FROM transactions");
        jdbcTemplate.execute("DELETE FROM wallet_cdi");
        jdbcTemplate.execute("DELETE FROM wallet_events");
        jdbcTemplate.execute("DELETE FROM wallets");

        Set<String> keys = redisTemplate.keys("*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
    private static final long DEFAULT_TIMEOUT_MS = 10_000;

    // ── System ────────────────────────────────────────────────────────────────

    @Given("the system is available")
    public void theSystemIsAvailable() {}

    // ── Wallet creation ───────────────────────────────────────────────────────

    @Given("a wallet exists for user {string}")
    public void aWalletExistsForUser(String userId) throws Exception {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wallets WHERE user_id = ?", Integer.class, userId);
        if (count == null || count == 0) {
            mockMvc.perform(post("/creation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("userId", userId))))
                    .andExpect(status().isOk());
        }
    }

    @When("I create a wallet for user {string}")
    public void iCreateAWalletForUser(String userId) throws Exception {
        lastResult = mockMvc.perform(post("/creation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", userId))))
                .andReturn();
    }

    @When("I create a wallet with null userId")
    public void iCreateAWalletWithNullUserId() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", null);
        lastResult = mockMvc.perform(post("/creation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
    }

    @When("I retrieve the balance for user {string}")
    public void iRetrieveTheBalanceForUser(String userId) throws Exception {
        lastResult = mockMvc.perform(get("/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", userId))))
                .andReturn();
    }

    // ── Deposit ───────────────────────────────────────────────────────────────

    @When("I deposit {string} for user {string}")
    public void iDepositForUser(String amount, String userId) throws Exception {
        requestStartTime = System.currentTimeMillis();
        lastResult = mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", userId,
                                "amount", new BigDecimal(amount),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andReturn();
    }

    @When("I make {int} deposits of {string} for user {string}")
    public void iMakeDepositsForUser(int count, String amount, String userId) throws Exception {
        for (int i = 0; i < count; i++) {
            mockMvc.perform(post("/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "userId", userId,
                                    "amount", new BigDecimal(amount),
                                    "idempotencyId", UUID.randomUUID().toString()))))
                    .andReturn();
        }
    }

    @When("I make {int} deposits of {string} for user {string} with the same idempotencyId")
    public void iMakeDepositsWithSameIdempotencyId(int count, String amount, String userId) throws Exception {
        reusableIdempotencyId = UUID.randomUUID().toString();
        for (int i = 0; i < count; i++) {
            lastResult = mockMvc.perform(post("/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "userId", userId,
                                    "amount", new BigDecimal(amount),
                                    "idempotencyId", reusableIdempotencyId))))
                    .andReturn();
        }
    }

    @When("I make {int} deposits of {string} for user {string} with different idempotencyIds")
    public void iMakeDepositsWithDifferentIdempotencyIds(int count, String amount, String userId) throws Exception {
        for (int i = 0; i < count; i++) {
            mockMvc.perform(post("/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "userId", userId,
                                    "amount", new BigDecimal(amount),
                                    "idempotencyId", UUID.randomUUID().toString()))))
                    .andReturn();
        }
    }

    @When("I deposit {string} for user {string} with fixed idempotencyId {string}")
    public void iDepositWithFixedIdempotencyId(String amount, String userId, String idempotencyId) throws Exception {
        reusableIdempotencyId = idempotencyId;
        lastResult = mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", userId,
                                "amount", new BigDecimal(amount),
                                "idempotencyId", idempotencyId))))
                .andReturn();
    }

    @When("I deposit {string} for user {string} with null idempotencyId")
    public void iDepositWithNullIdempotencyId(String amount, String userId) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("amount", new BigDecimal(amount));
        body.put("idempotencyId", null);
        lastResult = mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
    }

    @When("I deposit {string} for user {string} with idempotencyId {string}")
    public void iDepositWithIdempotencyId(String amount, String userId, String idempotencyId) throws Exception {
        lastResult = mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", userId,
                                "amount", new BigDecimal(amount),
                                "idempotencyId", idempotencyId))))
                .andReturn();
    }

    @And("I deposit {string} for user {string} and wait for processing")
    public void iDepositAndWaitForProcessing(String amount, String userId) throws Exception {
        reusableIdempotencyId = UUID.randomUUID().toString();
        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", userId,
                                "amount", new BigDecimal(amount),
                                "idempotencyId", reusableIdempotencyId))))
                .andReturn();
        waitForCondition(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type = 'DEPOSIT_COMPLETED' AND aggregate_id = '" + userId + "'",
                1, DEFAULT_TIMEOUT_MS);
    }

    @When("I send the same deposit again with the same idempotencyId for {string}")
    public void iSendTheSameDepositAgain(String userId) throws Exception {
        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", userId,
                                "amount", new BigDecimal("100.00"),
                                "idempotencyId", reusableIdempotencyId))))
                .andReturn();
    }

    // ── Withdraw ──────────────────────────────────────────────────────────────

    @When("I withdraw {string} for user {string}")
    public void iWithdrawForUser(String amount, String userId) throws Exception {
        lastResult = mockMvc.perform(post("/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", userId,
                                "amount", new BigDecimal(amount),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andReturn();
    }

    @When("I make {int} withdraws of {string} for user {string} with the same idempotencyId")
    public void iMakeWithdrawsWithSameIdempotencyId(int count, String amount, String userId) throws Exception {
        reusableIdempotencyId = UUID.randomUUID().toString();
        for (int i = 0; i < count; i++) {
            lastResult = mockMvc.perform(post("/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "userId", userId,
                                    "amount", new BigDecimal(amount),
                                    "idempotencyId", reusableIdempotencyId))))
                    .andReturn();
        }
    }

    // ── Balance setup ─────────────────────────────────────────────────────────

    @And("user {string} has a balance of {string}")
    public void userHasABalanceOf(String userId, String amount) throws Exception {
        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", userId,
                                "amount", new BigDecimal(amount),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andExpect(status().isOk());
        waitForExactBalance(userId, new BigDecimal(amount), DEFAULT_TIMEOUT_MS);
    }

    @And("user {string} made a withdraw of {string}")
    public void userMadeAWithdrawOf(String userId, String amount) throws Exception {
        iWithdrawForUser(amount, userId);
        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                        "(SELECT id FROM wallets WHERE user_id = '" + userId + "') AND type = 'WITHDRAW'",
                1, DEFAULT_TIMEOUT_MS);
    }

    @And("user {string} made a transfer of {string} to {string}")
    public void userMadeATransferOf(String fromUserId, String amount, String toUserId) throws Exception {
        iTransferFromTo(amount, fromUserId, toUserId);
        waitForCondition("SELECT COUNT(*) FROM transfers", 1, DEFAULT_TIMEOUT_MS);
    }

    @When("user {string} deposits {string}")
    public void userDeposits(String userId, String amount) throws Exception {
        iDepositForUser(amount, userId);
    }

    @When("user {string} withdraws {string}")
    public void userWithdraws(String userId, String amount) throws Exception {
        iWithdrawForUser(amount, userId);
    }

    // ── Transfer ──────────────────────────────────────────────────────────────

    @When("I transfer {string} from {string} to {string}")
    public void iTransferFromTo(String amount, String fromUserId, String toUserId) throws Exception {
        lastResult = mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fromUserId", fromUserId,
                                "toUserId", toUserId,
                                "amount", new BigDecimal(amount),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andReturn();
    }

    @When("I transfer {string} from {string} to {string} and capture the transactionId")
    public void iTransferAndCaptureTransactionId(String amount, String fromUserId, String toUserId) throws Exception {
        lastResult = mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fromUserId", fromUserId,
                                "toUserId", toUserId,
                                "amount", new BigDecimal(amount),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> response = objectMapper.readValue(
                lastResult.getResponse().getContentAsString(), Map.class);
        capturedTransferAggregateId = response.get("transactionId").toString();
    }

    @When("I make {int} transfers of {string} from {string} to {string} with the same idempotencyId")
    public void iMakeTransfersWithSameIdempotencyId(int count, String amount, String from, String to) throws Exception {
        reusableIdempotencyId = UUID.randomUUID().toString();
        for (int i = 0; i < count; i++) {
            mockMvc.perform(post("/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "fromUserId", from,
                                    "toUserId", to,
                                    "amount", new BigDecimal(amount),
                                    "idempotencyId", reusableIdempotencyId))))
                    .andReturn();
        }
    }

    // ── Generic operation ─────────────────────────────────────────────────────

    @When("I perform a generic operation of type {string} with amount {string} for user {string}")
    public void iPerformGenericOperation(String type, String amount, String userId) throws Exception {
        lastResult = mockMvc.perform(post("/any-operation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", userId,
                                "transactionType", type,
                                "amount", new BigDecimal(amount),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andReturn();
    }

    @When("I perform a generic transfer of {string} from {string} to {string}")
    public void iPerformGenericTransfer(String amount, String from, String to) throws Exception {
        lastResult = mockMvc.perform(post("/any-operation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fromUserId", from,
                                "toUserId", to,
                                "transactionType", "TRANSFER",
                                "amount", new BigDecimal(amount),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andReturn();
    }

    // ── History ───────────────────────────────────────────────────────────────

    @When("I retrieve the history for user {string} for today")
    public void iRetrieveHistoryForToday(String userId) throws Exception {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 00:00:00.000";
        lastResult = mockMvc.perform(get("/history")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", userId, "date", today))))
                .andReturn();
    }

    @When("I retrieve the history for user {string} for date {string}")
    public void iRetrieveHistoryForDate(String userId, String date) throws Exception {
        lastResult = mockMvc.perform(get("/history")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", userId, "date", date))))
                .andReturn();
    }

    // ── Concurrency ───────────────────────────────────────────────────────────

    @When("{int} threads concurrently send deposits of {string} for {string}")
    public void threadsConcurrentlyDeposit(int threadCount, String amount, String userId) throws InterruptedException {
        fireConcurrentDeposits(threadCount, amount, userId);
    }

    @When("{int} threads concurrently send withdraws of {string} for {string}")
    public void threadsConcurrentlyWithdraw(int threadCount, String amount, String userId) throws InterruptedException {
        List<Map<String, Object>> bodies = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            bodies.add(Map.of("userId", userId, "amount", new BigDecimal(amount),
                    "idempotencyId", UUID.randomUUID().toString()));
        }
        fireConcurrentRequests("/withdraw", bodies);
    }

    @When("{int} threads concurrently send the same deposit of {string} for {string}")
    public void threadsConcurrentlySendSameDeposit(int threadCount, String amount, String userId) throws InterruptedException {
        String idempotencyId = UUID.randomUUID().toString();
        List<Map<String, Object>> bodies = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            bodies.add(Map.of("userId", userId, "amount", new BigDecimal(amount), "idempotencyId", idempotencyId));
        }
        fireConcurrentRequests("/deposit", bodies);
    }

    @When("{int} threads concurrently send transfers of {string} from {string} to {string}")
    public void threadsConcurrentlyTransfer(int threadCount, String amount, String from, String to) throws InterruptedException {
        List<Map<String, Object>> bodies = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            bodies.add(Map.of("fromUserId", from, "toUserId", to,
                    "amount", new BigDecimal(amount), "idempotencyId", UUID.randomUUID().toString()));
        }
        fireConcurrentRequests("/transfer", bodies);
    }

    @When("{int} threads send deposits of {string} and {int} threads send withdraws of {string} concurrently for {string}")
    public void threadsSendDepositAndWithdrawConcurrently(int depositThreads, String depositAmount,
                                                          int withdrawThreads, String withdrawAmount,
                                                          String userId) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(depositThreads + withdrawThreads);
        CountDownLatch gate = new CountDownLatch(1);
        for (int i = 0; i < depositThreads; i++) {
            final String id = UUID.randomUUID().toString();
            executor.submit(() -> { try { gate.await(); mockMvc.perform(post("/deposit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("userId", userId,
                            "amount", new BigDecimal(depositAmount), "idempotencyId", id)))).andReturn();
            } catch (Exception ignored) {} });
        }
        for (int i = 0; i < withdrawThreads; i++) {
            final String id = UUID.randomUUID().toString();
            executor.submit(() -> { try { gate.await(); mockMvc.perform(post("/withdraw")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("userId", userId,
                            "amount", new BigDecimal(withdrawAmount), "idempotencyId", id)))).andReturn();
            } catch (Exception ignored) {} });
        }
        gate.countDown();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
    }

    @When("{int} threads transfer {string} from {string} to {string} and {int} threads transfer {string} from {string} to {string} concurrently")
    public void threadsBidirectionalTransfer(int t1, String a1, String from1, String to1,
                                             int t2, String a2, String from2, String to2) throws InterruptedException {
        List<Map<String, Object>> all = new ArrayList<>();
        for (int i = 0; i < t1; i++) {
            all.add(Map.of("fromUserId", from1, "toUserId", to1,
                    "amount", new BigDecimal(a1), "idempotencyId", UUID.randomUUID().toString()));
        }
        for (int i = 0; i < t2; i++) {
            all.add(Map.of("fromUserId", from2, "toUserId", to2,
                    "amount", new BigDecimal(a2), "idempotencyId", UUID.randomUUID().toString()));
        }
        fireConcurrentRequests("/transfer", all);
    }

    // ── Redis ─────────────────────────────────────────────────────────────────

    @When("all Redis keys are deleted")
    public void allRedisKeysAreDeleted() {
        Set<String> keys = redisTemplate.keys("*");
        if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys);
    }

    // ── Assertions — HTTP Status ──────────────────────────────────────────────

    @Then("the response should return status {int}")
    public void theResponseShouldReturnStatus(int status) {
        assertThat(lastResult.getResponse().getStatus()).isEqualTo(status);
    }

    @Then("the response should return status 4xx")
    public void theResponseShouldReturnStatus4xx() {
        assertThat(lastResult.getResponse().getStatus()).isBetween(400, 499);
    }

    @Then("the response should contain a {string}")
    public void theResponseShouldContainField(String field) throws Exception {
        String body = lastResult.getResponse().getContentAsString();
        assertThat(body).contains(field);
    }

    @Then("the response should contain userId {string}")
    public void theResponseShouldContainUserId(String userId) throws Exception {
        String body = lastResult.getResponse().getContentAsString();
        assertThat(body).contains(userId);
    }

    @Then("the response should contain balance {string}")
    public void theResponseShouldContainBalance(String balance) throws Exception {
        String body = lastResult.getResponse().getContentAsString();
        assertThat(body).contains(balance);
    }

    // ── Assertions — Balance ──────────────────────────────────────────────────

    @Then("the balance of {string} should be {string}")
    public void theBalanceShouldBe(String userId, String expected) {
        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = ?", BigDecimal.class, userId);
        assertThat(balance).isEqualByComparingTo(expected);
    }

    @Then("the balance of {string} should be {string} within {int} seconds")
    public void theBalanceShouldBeWithin(String userId, String expected, int seconds) throws InterruptedException {
        waitForExactBalance(userId, new BigDecimal(expected), (long) seconds * 1000);
    }

    @Then("the balance of {string} should be greater than or equal to {string}")
    public void theBalanceShouldBeGreaterThanOrEqualTo(String userId, String minValue) {
        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = ?", BigDecimal.class, userId);
        assertThat(balance).isGreaterThanOrEqualTo(new BigDecimal(minValue));
    }

    @Then("after waiting 3 seconds the balance of {string} should remain {string}")
    public void afterWaitingBalanceShouldRemain(String userId, String expected) throws InterruptedException {
        Thread.sleep(3000);
        theBalanceShouldBe(userId, expected);
    }

    @Then("the sum of balances of {string} and {string} should be {string}")
    public void sumOfBalancesShouldBe(String userA, String userB, String expected) {
        BigDecimal balA = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = ?", BigDecimal.class, userA);
        BigDecimal balB = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = ?", BigDecimal.class, userB);
        assertThat(balA.add(balB)).isEqualByComparingTo(expected);
    }

    @Then("the total sum of all balances should be {string}")
    public void totalSumOfAllBalancesShouldBe(String expected) {
        BigDecimal total = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(balance), 0) FROM wallets", BigDecimal.class);
        assertThat(total).isEqualByComparingTo(expected);
    }

    // ── Assertions — Database ─────────────────────────────────────────────────

    @Then("there should be {int} wallet in the database for user {string}")
    public void thereShouldBeWalletInDatabase(int count, String userId) {
        Integer actual = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wallets WHERE user_id = ?", Integer.class, userId);
        assertThat(actual).isEqualTo(count);
    }

    @Then("there should be only {int} wallet in the database for user {string}")
    public void thereShouldBeOnlyWalletInDatabase(int count, String userId) {
        thereShouldBeWalletInDatabase(count, userId);
    }

    @Then("there should be {int} transaction of type {string} for user {string}")
    public void thereShouldBeTransactionOfType(int count, String type, String userId) {
        Integer actual = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                        "(SELECT id FROM wallets WHERE user_id = ?) AND type = ?",
                Integer.class, userId, type);
        assertThat(actual).isEqualTo(count);
    }

    @Then("there should be {int} transaction of type {string} for user {string} within {int} seconds")
    public void thereShouldBeTransactionOfTypeWithin(int count, String type, String userId, int seconds) throws InterruptedException {
        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                        "(SELECT id FROM wallets WHERE user_id = '" + userId + "') AND type = '" + type + "'",
                count, (long) seconds * 1000);
        thereShouldBeTransactionOfType(count, type, userId);
    }

    @Then("there should be {int} transactions of type {string} for user {string} within {int} seconds")
    public void thereShouldBeTransactionsOfTypeWithin(int count, String type, String userId, int seconds) throws InterruptedException {
        thereShouldBeTransactionOfTypeWithin(count, type, userId, seconds);
    }

    @Then("there should be {int} transaction for user {string} within {int} seconds")
    public void thereShouldBeTransactionForUserWithin(int count, String userId, int seconds) throws InterruptedException {
        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                        "(SELECT id FROM wallets WHERE user_id = '" + userId + "')",
                count, (long) seconds * 1000);
    }

    @Then("there should be {int} transactions for user {string} within {int} seconds")
    public void thereShouldBeTransactionsForUserWithin(int count, String userId, int seconds) throws InterruptedException {
        thereShouldBeTransactionForUserWithin(count, userId, seconds);
    }

    @Then("there should be {int} transactions linked to the transfer within {int} seconds")
    public void thereShouldBeTransactionsLinkedToTransferWithin(int count, int seconds) throws InterruptedException, UnsupportedEncodingException, JsonProcessingException {
        Map<?, ?> response = objectMapper.readValue(
                lastResult.getResponse().getContentAsString(), Map.class);
        String trackId = response.get("transactionId").toString();
        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE transaction_track_id = '" + trackId + "'",
                count, (long) seconds * 1000);
    }

    @Then("there should be no transactions linked to the transfer")
    public void thereShouldBeNoTransactionsLinkedToTransfer() throws Exception {
        Map<?, ?> response = objectMapper.readValue(
                lastResult.getResponse().getContentAsString(), Map.class);
        String trackId = response.get("transactionId").toString();
        Thread.sleep(3000);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE transaction_track_id = ?",
                Integer.class, trackId);
        assertThat(count).isZero();
    }

    @Then("there should be exactly {int} transaction of type {string} for user {string}")
    public void thereShouldBeExactlyTransactionOfType(int count, String type, String userId) {
        thereShouldBeTransactionOfType(count, type, userId);
    }

    @Then("after waiting 3 seconds there should be no transactions in the database")
    public void afterWaitingNoTransactionsInDatabase() throws InterruptedException {
        Thread.sleep(3000);
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transactions", Integer.class);
        assertThat(count).isZero();
    }

    @Then("after waiting 3 seconds there should be no {string} transactions for user {string}")
    public void afterWaitingNoTransactionsOfTypeForUser(String type, String userId) throws InterruptedException {
        Thread.sleep(3000);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                        "(SELECT id FROM wallets WHERE user_id = ?) AND type = ?",
                Integer.class, userId, type);
        assertThat(count).isZero();
    }

    @Then("after waiting 3 seconds there should be no transfers in the database")
    public void afterWaitingNoTransfersInDatabase() throws InterruptedException {
        Thread.sleep(3000);
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transfers", Integer.class);
        assertThat(count).isZero();
    }

    @Then("there should be {int} transfer record in the database within {int} seconds")
    public void thereShouldBeTransferRecordWithin(int count, int seconds) throws InterruptedException {
        waitForCondition("SELECT COUNT(*) FROM transfers", count, (long) seconds * 1000);
    }

    @Then("there should be {int} transfer records in the database within {int} seconds")
    public void thereShouldBeTransferRecordsWithin(int count, int seconds) throws InterruptedException {
        thereShouldBeTransferRecordWithin(count, seconds);
    }

    @Then("the transfer record should have distinct debit_transaction_id and credit_transaction_id")
    public void transferRecordShouldHaveDistinctIds() {
        Map<String, Object> transfer = jdbcTemplate.queryForMap(
                "SELECT debit_transaction_id, credit_transaction_id FROM transfers");
        assertThat(transfer.get("debit_transaction_id")).isNotNull();
        assertThat(transfer.get("credit_transaction_id")).isNotNull();
        assertThat(transfer.get("debit_transaction_id")).isNotEqualTo(transfer.get("credit_transaction_id"));
    }

    // ── Assertions — Balance Snapshots ────────────────────────────────────────

    @Then("the balance before the transaction for {string} should be {string}")
    public void balanceBeforeTransactionShouldBe(String userId, String expected) {
        Map<String, Object> tx = jdbcTemplate.queryForMap(
                "SELECT balance_before_transaction FROM transactions " +
                        "WHERE wallet_id = (SELECT id FROM wallets WHERE user_id = ?) LIMIT 1", userId);
        assertThat((BigDecimal) tx.get("balance_before_transaction")).isEqualByComparingTo(expected);
    }

    @Then("the balance after the transaction for {string} should be {string}")
    public void balanceAfterTransactionShouldBe(String userId, String expected) {
        Map<String, Object> tx = jdbcTemplate.queryForMap(
                "SELECT balance_after_transaction FROM transactions " +
                        "WHERE wallet_id = (SELECT id FROM wallets WHERE user_id = ?) LIMIT 1", userId);
        assertThat((BigDecimal) tx.get("balance_after_transaction")).isEqualByComparingTo(expected);
    }

    @Then("the balance before the withdraw transaction for {string} should be {string}")
    public void balanceBeforeWithdrawShouldBe(String userId, String expected) {
        Map<String, Object> tx = jdbcTemplate.queryForMap(
                "SELECT balance_before_transaction FROM transactions " +
                        "WHERE wallet_id = (SELECT id FROM wallets WHERE user_id = ?) AND type = 'WITHDRAW'", userId);
        assertThat((BigDecimal) tx.get("balance_before_transaction")).isEqualByComparingTo(expected);
    }

    @Then("the balance after the withdraw transaction for {string} should be {string}")
    public void balanceAfterWithdrawShouldBe(String userId, String expected) {
        Map<String, Object> tx = jdbcTemplate.queryForMap(
                "SELECT balance_after_transaction FROM transactions " +
                        "WHERE wallet_id = (SELECT id FROM wallets WHERE user_id = ?) AND type = 'WITHDRAW'", userId);
        assertThat((BigDecimal) tx.get("balance_after_transaction")).isEqualByComparingTo(expected);
    }

    // ── Assertions — Response Time ────────────────────────────────────────────

    @Then("the response time should be less than {int} milliseconds")
    public void responseTimeShouldBeLessThan(int ms) {
        long elapsed = System.currentTimeMillis() - requestStartTime;
        assertThat(elapsed).isLessThan(ms);
    }

    // ── Assertions — History ──────────────────────────────────────────────────

    @Then("the response should contain a list with {int} transaction")
    public void responseContainsListWithTransaction(int count) throws Exception {
        String body = lastResult.getResponse().getContentAsString();
        List<?> list = objectMapper.readValue(body, List.class);
        assertThat(list).hasSize(count);
    }

    @Then("the response should contain a list with {int} transactions")
    public void responseContainsListWithTransactions(int count) throws Exception {
        responseContainsListWithTransaction(count);
    }

    @Then("the response should contain an empty list")
    public void responseContainsEmptyList() throws Exception {
        String body = lastResult.getResponse().getContentAsString();
        List<?> list = objectMapper.readValue(body, List.class);
        assertThat(list).isEmpty();
    }

    @Then("the first transaction should have balanceBeforeTransaction equal to {string}")
    public void firstTransactionBalanceBefore(String expected) throws Exception {
        String body = lastResult.getResponse().getContentAsString();
        List<Map<?, ?>> list = objectMapper.readValue(body, List.class);
        assertThat(list.get(0).get("balanceBeforeTransaction").toString()).isEqualTo(expected);
    }

    @Then("the first transaction should have balanceAfterTransaction equal to {string}")
    public void firstTransactionBalanceAfter(String expected) throws Exception {
        String body = lastResult.getResponse().getContentAsString();
        List<Map<?, ?>> list = objectMapper.readValue(body, List.class);
        assertThat(list.get(0).get("balanceAfterTransaction").toString()).isEqualTo(expected);
    }

    // ── Assertions — Event Sourcing ───────────────────────────────────────────

    @Then("there should be exactly {int} event of type {string} for aggregate {string}")
    public void thereShouldBeExactlyEventForAggregate(int count, String eventType, String aggregateId) {
        Integer actual = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type = ? AND aggregate_id = ?",
                Integer.class, eventType, aggregateId);
        assertThat(actual).isEqualTo(count);
    }

    @Then("there should be exactly {int} event of type {string} for aggregate {string} within {int} seconds")
    public void thereShouldBeExactlyEventForAggregateWithin(int count, String eventType, String aggregateId, int seconds) throws InterruptedException {
        waitForCondition(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type = '" + eventType +
                        "' AND aggregate_id = '" + aggregateId + "'",
                count, (long) seconds * 1000);
        thereShouldBeExactlyEventForAggregate(count, eventType, aggregateId);
    }

    @Then("there should be no event of type {string} for aggregate {string}")
    public void thereShouldBeNoEventForAggregate(String eventType, String aggregateId) {
        Integer actual = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type = ? AND aggregate_id = ?",
                Integer.class, eventType, aggregateId);
        assertThat(actual).isZero();
    }

    @Then("there should be exactly {int} event of type {string} for the transfer aggregate within {int} seconds")
    public void thereShouldBeEventForTransferAggregateWithin(int count, String eventType, int seconds) throws InterruptedException {
        waitForCondition(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type = '" + eventType +
                        "' AND aggregate_type = 'TRANSFER' AND aggregate_id = '" + capturedTransferAggregateId + "'",
                count, (long) seconds * 1000);
        Integer actual = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type = ? AND aggregate_type = 'TRANSFER' AND aggregate_id = ?",
                Integer.class, eventType, capturedTransferAggregateId);
        assertThat(actual).isEqualTo(count);
    }

    @Then("there should be exactly {int} event of type {string} for the transfer aggregate")
    public void thereShouldBeEventForTransferAggregate(int count, String eventType) {
        Integer actual = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type = ? AND aggregate_type = 'TRANSFER' AND aggregate_id = ?",
                Integer.class, eventType, capturedTransferAggregateId);
        assertThat(actual).isEqualTo(count);
    }

    @Then("there should be no retry events for the transfer aggregate")
    public void thereShouldBeNoRetryEventsForTransferAggregate() {
        Integer actual = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type = 'TRANSACTION_RETRY_ATTEMPTED' AND aggregate_id = ?",
                Integer.class, capturedTransferAggregateId);
        assertThat(actual).isZero();
    }

    @Then("there should be no events of types {string}, {string} or {string} for {string}")
    public void thereShouldBeNoEventsOfTypes(String t1, String t2, String t3, String aggregateId) {
        Integer actual = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type IN (?, ?, ?) AND aggregate_id = ?",
                Integer.class, t1, t2, t3, aggregateId);
        assertThat(actual).isZero();
    }

    @Then("the total event count for aggregate {string} should be exactly {int}")
    public void totalEventCountForAggregateShouldBe(String aggregateId, int count) {
        Integer actual = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wallet_events WHERE aggregate_id = ?", Integer.class, aggregateId);
        assertThat(actual).isEqualTo(count);
    }

    @Then("the payload of event {string} for {string} should have balanceBefore equal to {string}")
    public void payloadShouldHaveBalanceBefore(String eventType, String aggregateId, String expected) {
        String value = jdbcTemplate.queryForObject(
                "SELECT payload->>'balanceBefore' FROM wallet_events WHERE event_type = ? AND aggregate_id = ?",
                String.class, eventType, aggregateId);
        assertThat(value).isEqualTo(expected);
    }

    @Then("the payload of event {string} for {string} should have balanceAfter equal to {string}")
    public void payloadShouldHaveBalanceAfter(String eventType, String aggregateId, String expected) {
        String value = jdbcTemplate.queryForObject(
                "SELECT payload->>'balanceAfter' FROM wallet_events WHERE event_type = ? AND aggregate_id = ?",
                String.class, eventType, aggregateId);
        assertThat(value).isEqualTo(expected);
    }

    @Then("the payload of event {string} for the transfer aggregate should contain:")
    public void payloadOfTransferAggregateShouldContain(String eventType, io.cucumber.datatable.DataTable table) {
        Map<String, Object> fields = jdbcTemplate.queryForMap(
                "SELECT payload->>'fromBalanceBefore' AS fbb, payload->>'fromBalanceAfter' AS fba," +
                        "       payload->>'toBalanceBefore' AS tbb, payload->>'toBalanceAfter' AS tba " +
                        "FROM wallet_events WHERE event_type = ? AND aggregate_type = 'TRANSFER' AND aggregate_id = ?",
                eventType, capturedTransferAggregateId);

        table.asMaps().forEach(row -> {
            switch (row.get("field")) {
                case "fromBalanceBefore" -> assertThat(fields.get("fbb").toString()).isEqualTo(row.get("value"));
                case "fromBalanceAfter"  -> assertThat(fields.get("fba").toString()).isEqualTo(row.get("value"));
                case "toBalanceBefore"   -> assertThat(fields.get("tbb").toString()).isEqualTo(row.get("value"));
                case "toBalanceAfter"    -> assertThat(fields.get("tba").toString()).isEqualTo(row.get("value"));
            }
        });
    }

    @Then("the payload of event {string} for {string} should have reason containing {string}")
    public void payloadShouldHaveReasonContaining(String eventType, String aggregateId, String expectedSubstring) {
        String value = jdbcTemplate.queryForObject(
                "SELECT payload->>'reason' FROM wallet_events WHERE event_type = ? AND aggregate_id = ?",
                String.class, eventType, aggregateId);
        assertThat(value).contains(expectedSubstring);
    }

    @Then("the payload of event {string} for {string} should have retryCount equal to {string}")
    public void payloadShouldHaveRetryCount(String eventType, String aggregateId, String expected) {
        String value = jdbcTemplate.queryForObject(
                "SELECT payload->>'retryCount' FROM wallet_events WHERE event_type = ? AND aggregate_id = ?",
                String.class, eventType, aggregateId);
        assertThat(value).isEqualTo(expected);
    }

    @Then("the payload of event {string} for {string} should have transactionType equal to {string}")
    public void payloadShouldHaveTransactionType(String eventType, String aggregateId, String expected) {
        String value = jdbcTemplate.queryForObject(
                "SELECT payload->>'transactionType' FROM wallet_events WHERE event_type = ? AND aggregate_id = ?",
                String.class, eventType, aggregateId);
        assertThat(value).isEqualTo(expected);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void waitForCondition(String sql, int expectedCount, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            if (count != null && count == expectedCount) return;
            Thread.sleep(200);
        }
        throw new AssertionError("Condition not met within " + timeoutMs + "ms. SQL: " + sql);
    }

    private void waitForExactBalance(String userId, BigDecimal expected, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                BigDecimal actual = jdbcTemplate.queryForObject(
                        "SELECT balance FROM wallets WHERE user_id = ?", BigDecimal.class, userId);
                if (actual != null && actual.compareTo(expected) == 0) return;
            } catch (Exception ignored) {}
            Thread.sleep(200);
        }
        throw new AssertionError("Balance never reached " + expected + " for user=" + userId);
    }

    private void fireConcurrentDeposits(int threadCount, String amount, String userId) throws InterruptedException {
        List<Map<String, Object>> bodies = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            bodies.add(Map.of("userId", userId, "amount", new BigDecimal(amount),
                    "idempotencyId", UUID.randomUUID().toString()));
        }
        fireConcurrentRequests("/deposit", bodies);
    }

    private void fireConcurrentRequests(String endpoint, List<Map<String, Object>> bodies) throws InterruptedException {
        int n = bodies.size();
        ExecutorService executor = Executors.newFixedThreadPool(n);
        CountDownLatch startGate = new CountDownLatch(1);
        for (Map<String, Object> body : bodies) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    mockMvc.perform(post(endpoint)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(new ObjectMapper().writeValueAsString(body)))
                            .andReturn();
                } catch (Exception ignored) {}
            });
        }
        startGate.countDown();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
    }
}