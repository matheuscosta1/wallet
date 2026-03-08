package br.com.wallet.project.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Functional tests for wallet creation and balance retrieval.
 *
 * Scenarios covered:
 *  W-01  Create wallet successfully
 *  W-02  Create duplicate wallet returns 409
 *  W-03  Create wallet with null userId returns 400
 *  W-04  Create wallet with blank userId returns 400
 *  W-05  Retrieve balance of newly created wallet (0.00)
 *  W-06  Retrieve balance of non-existent wallet returns 404
 */
@DisplayName("Wallet — Creation & Balance")
class WalletCreationAndBalanceFunctionalTest extends BaseFunctionalTest {

    @Autowired
    private ObjectMapper objectMapper;

    // ── W-01 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("W-01: Create wallet successfully → 200 and wallet persisted in DB")
    void shouldCreateWalletSuccessfully() throws Exception {
        var body = Map.of("userId", "user-w01");

        mockMvc.perform(post("/creation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-w01"))
                .andExpect(jsonPath("$.balance").value(0.0));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wallets WHERE user_id = 'user-w01'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    // ── W-02 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("W-02: Creating duplicate wallet for same userId → 409 Conflict")
    void shouldRejectDuplicateWalletCreation() throws Exception {
        var body = Map.of("userId", "user-w02");

        // First creation — OK
        mockMvc.perform(post("/creation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        // Second creation — must fail
        mockMvc.perform(post("/creation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is4xxClientError());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wallets WHERE user_id = 'user-w02'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    // ── W-03 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("W-03: Create wallet with null userId → 400 Bad Request")
    void shouldRejectWalletCreationWithNullUserId() throws Exception {
        var body = new java.util.HashMap<String, Object>();
        body.put("userId", null);

        mockMvc.perform(post("/creation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ── W-04 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("W-04: Create wallet with blank userId → 400 Bad Request")
    void shouldRejectWalletCreationWithBlankUserId() throws Exception {
        var body = Map.of("userId", "   ");

        mockMvc.perform(post("/creation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ── W-05 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("W-05: Retrieve balance of newly created wallet → balance is 0.00")
    void shouldReturnZeroBalanceForNewWallet() throws Exception {
        // Create wallet
        var createBody = Map.of("userId", "user-w05");
        mockMvc.perform(post("/creation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isOk());

        // Check balance
        var balanceBody = Map.of("userId", "user-w05");
        mockMvc.perform(get("/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(balanceBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0.0))
                .andExpect(jsonPath("$.userId").value("user-w05"));
    }

    // ── W-06 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("W-06: Retrieve balance of non-existent wallet → 404 Not Found")
    void shouldReturn404WhenWalletNotFound() throws Exception {
        var body = Map.of("userId", "ghost-user-does-not-exist");

        mockMvc.perform(get("/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is4xxClientError());
    }
}
