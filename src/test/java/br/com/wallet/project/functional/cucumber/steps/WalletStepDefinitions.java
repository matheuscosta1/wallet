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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;

/**
 * Step definitions unificadas para todos os cenários Cucumber.
 *
 * Cobre os domínios:
 *  - Criação de carteira e consulta de saldo (W-*)
 *  - Depósito (D-*)
 *  - Saque (WD-*)
 *  - Transferência (T-*)
 *  - Idempotência (I-*)
 *  - Concorrência (C-*)
 *  - Histórico e endpoint genérico (H-*, G-*)
 *  - Event Sourcing (ES-*)
 *  - End-to-end (E-*)
 */
public class WalletStepDefinitions {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private RedisTemplate<String, String> redisTemplate;
    @Autowired private ObjectMapper objectMapper;

    // ── Estado por cenário ────────────────────────────────────────────────────

    private MvcResult lastResult;
    private long requestStartTime;

    // Controla idempotencyId reutilizável entre steps do mesmo cenário
    private String reusableIdempotencyId;
    // Armazena o transactionId capturado de uma transferência
    private String capturedTransferAggregateId;

    private static final long DEFAULT_TIMEOUT_MS = 10_000;

    // ── Infraestrutura ────────────────────────────────────────────────────────

    @Dado("que o sistema está disponível")
    public void sistemaDisponivel() {
        // A disponibilidade é garantida pelo contexto Spring; sem ação necessária.
    }

    // ── Criação de carteira ───────────────────────────────────────────────────

    @Dado("que existe uma carteira para o usuário {string}")
    public void existeCarteiraParaUsuario(String userId) throws Exception {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wallets WHERE user_id = ?", Integer.class, userId);
        if (count == null || count == 0) {
            mockMvc.perform(post("/creation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("userId", userId))))
                    .andExpect(status().isOk());
        }
    }

    @Quando("eu crio uma carteira para o usuário {string}")
    public void crioCarteiraParaUsuario(String userId) throws Exception {
        Object body = userId.isBlank()
                ? Map.of("userId", userId)
                : Map.of("userId", userId);
        lastResult = mockMvc.perform(post("/creation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
    }

    @Quando("eu crio uma carteira com userId nulo")
    public void crioCarteiraComUserIdNulo() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", null);
        lastResult = mockMvc.perform(post("/creation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
    }

    @Quando("eu consulto o saldo do usuário {string}")
    public void consultaSaldoUsuario(String userId) throws Exception {
        lastResult = mockMvc.perform(get("/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", userId))))
                .andReturn();
    }

    // ── Depósito ──────────────────────────────────────────────────────────────

    @Quando("eu realizo um depósito de {string} para o usuário {string}")
    public void realizoDeposito(String amount, String userId) throws Exception {
        requestStartTime = System.currentTimeMillis();
        lastResult = mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", userId,
                                "amount", new BigDecimal(amount),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andReturn();
    }

    @Quando("eu realizo {int} depósitos de {string} para o usuário {string}")
    public void realizoMultiplosDepositos(int count, String amount, String userId) throws Exception {
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

    @Quando("eu realizo {int} depósitos de {string} para o usuário {string} com o mesmo idempotencyId")
    public void realizoDepositosMesmoIdempotencyId(int count, String amount, String userId) throws Exception {
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

    @Quando("eu realizo {int} depósitos de {string} para o usuário {string} com idempotencyIds diferentes")
    public void realizoDepositosIdempotencyIdsDiferentes(int count, String amount, String userId) throws Exception {
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

    @Quando("eu realizo um depósito de {string} para o usuário {string} com idempotencyId fixo {string}")
    public void realizoDepositoIdempotencyIdFixo(String amount, String userId, String idempotencyId) throws Exception {
        reusableIdempotencyId = idempotencyId;
        lastResult = mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", userId,
                                "amount", new BigDecimal(amount),
                                "idempotencyId", idempotencyId))))
                .andReturn();
    }

    @Quando("eu realizo um depósito de {string} para o usuário {string} com idempotencyId nulo")
    public void realizoDepositoIdempotencyIdNulo(String amount, String userId) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("amount", new BigDecimal(amount));
        body.put("idempotencyId", null);
        lastResult = mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
    }

    @Quando("eu realizo um depósito de {string} para o usuário {string} com idempotencyId {string}")
    public void realizoDepositoComIdempotencyId(String amount, String userId, String idempotencyId) throws Exception {
        lastResult = mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", userId,
                                "amount", new BigDecimal(amount),
                                "idempotencyId", idempotencyId))))
                .andReturn();
    }

    @Quando("eu realizo um depósito de {string} para o usuário {string} e aguardo o processamento")
    public void realizoDepositoEAguardo(String amount, String userId) throws Exception {
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

    @Quando("eu realizo novamente o mesmo depósito com o mesmo idempotencyId para {string}")
    public void realizoDepositoDuplicado(String userId) throws Exception {
        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", userId,
                                "amount", new BigDecimal("100.00"),
                                "idempotencyId", reusableIdempotencyId))))
                .andReturn();
    }

    // ── Saque ─────────────────────────────────────────────────────────────────

    @Quando("eu realizo um saque de {string} para o usuário {string}")
    public void realizoSaque(String amount, String userId) throws Exception {
        lastResult = mockMvc.perform(post("/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", userId,
                                "amount", new BigDecimal(amount),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andReturn();
    }

    @Quando("eu realizo {int} saques de {string} para o usuário {string} com o mesmo idempotencyId")
    public void realizoSaquesMesmoIdempotencyId(int count, String amount, String userId) throws Exception {
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

    @Quando("o usuário {string} deposita {string}")
    public void usuarioDeposita(String userId, String amount) throws Exception {
        realizoDeposito(amount, userId);
    }

    @Quando("o usuário {string} saca {string}")
    public void usuarioSaca(String userId, String amount) throws Exception {
        realizoSaque(amount, userId);
    }

    // ── Saldo Inicial (via depósito) ──────────────────────────────────────────

    @Dado("o usuário {string} tem saldo de {string}")
    public void usuarioTemSaldo(String userId, String amount) throws Exception {
        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", userId,
                                "amount", new BigDecimal(amount),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andExpect(status().isOk());
        waitForExactBalance(userId, new BigDecimal(amount), DEFAULT_TIMEOUT_MS);
    }

    @Dado("o usuário {string} realizou um saque de {string}")
    public void usuarioRealizouSaque(String userId, String amount) throws Exception {
        realizoSaque(amount, userId);
        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = '" + userId + "') AND type = 'WITHDRAW'",
                1, DEFAULT_TIMEOUT_MS);
    }

    @Dado("o usuário {string} realizou uma transferência de {string} para {string}")
    public void usuarioRealizouTransferencia(String fromUserId, String amount, String toUserId) throws Exception {
        realizoTransferencia(amount, fromUserId, toUserId);
        waitForCondition("SELECT COUNT(*) FROM transfers", 1, DEFAULT_TIMEOUT_MS);
    }

    // ── Transferência ─────────────────────────────────────────────────────────

    @Quando("eu transfiro {string} de {string} para {string}")
    public void realizoTransferencia(String amount, String fromUserId, String toUserId) throws Exception {
        lastResult = mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fromUserId", fromUserId,
                                "toUserId", toUserId,
                                "amount", new BigDecimal(amount),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andReturn();
    }

    @Quando("eu transfiro {string} de {string} para {string} e capturo o transactionId")
    public void realizoTransferenciaECapturoId(String amount, String fromUserId, String toUserId) throws Exception {
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
                lastResult.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8), Map.class);
        capturedTransferAggregateId = response.get("transactionId").toString();
    }

    @Quando("eu realizo {int} transferências de {string} de {string} para {string} com o mesmo idempotencyId")
    public void realizoTransferenciasMesmoIdempotencyId(int count, String amount, String from, String to) throws Exception {
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

    // ── Operação genérica ─────────────────────────────────────────────────────

    @Quando("eu realizo uma operação genérica do tipo {string} de {string} para o usuário {string}")
    public void realizoOperacaoGenerica(String type, String amount, String userId) throws Exception {
        lastResult = mockMvc.perform(post("/any-operation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", userId,
                                "transactionType", type,
                                "amount", new BigDecimal(amount),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andReturn();
    }

    @Quando("eu realizo uma operação genérica de transferência de {string} de {string} para {string}")
    public void realizoOperacaoGenericaTransferencia(String amount, String from, String to) throws Exception {
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

    // ── Histórico ─────────────────────────────────────────────────────────────

    @Quando("eu consulto o histórico do usuário {string} para a data de hoje")
    public void consultaHistoricoHoje(String userId) throws Exception {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 00:00:00.000";
        lastResult = mockMvc.perform(get("/history")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", userId, "date", today))))
                .andReturn();
    }

    @Quando("eu consulto o histórico do usuário {string} para a data {string}")
    public void consultaHistoricoData(String userId, String date) throws Exception {
        lastResult = mockMvc.perform(get("/history")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", userId, "date", date))))
                .andReturn();
    }

    // ── Concorrência ──────────────────────────────────────────────────────────

    @Quando("{int} threads enviam simultaneamente depósitos de {string} para {string}")
    public void threadsEnviamDepositos(int threadCount, String amount, String userId) throws Exception {
        fireConcurrentDeposits(threadCount, amount, userId);
    }

    @Quando("{int} threads enviam simultaneamente saques de {string} para {string}")
    public void threadsEnviamSaques(int threadCount, String amount, String userId) throws Exception {
        List<Map<String, Object>> bodies = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            bodies.add(Map.of(
                    "userId", userId,
                    "amount", new BigDecimal(amount),
                    "idempotencyId", UUID.randomUUID().toString()));
        }
        fireConcurrentRequests("/withdraw", bodies);
    }

    @Quando("{int} threads enviam simultaneamente o mesmo depósito de {string} para {string}")
    public void threadsEnviamMesmoDeposito(int threadCount, String amount, String userId) throws Exception {
        String idempotencyId = UUID.randomUUID().toString();
        List<Map<String, Object>> bodies = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            bodies.add(Map.of(
                    "userId", userId,
                    "amount", new BigDecimal(amount),
                    "idempotencyId", idempotencyId));
        }
        fireConcurrentRequests("/deposit", bodies);
    }

    @Quando("{int} threads enviam simultaneamente transferências de {string} de {string} para {string}")
    public void threadsEnviamTransferencias(int threadCount, String amount, String from, String to) throws Exception {
        List<Map<String, Object>> bodies = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            bodies.add(Map.of(
                    "fromUserId", from,
                    "toUserId", to,
                    "amount", new BigDecimal(amount),
                    "idempotencyId", UUID.randomUUID().toString()));
        }
        fireConcurrentRequests("/transfer", bodies);
    }

    @Quando("{int} threads enviam depósitos de {string} e {int} threads enviam saques de {string} concorrentemente para {string}")
    public void threadsEnviamDepositosESaques(int depositThreads, String depositAmount,
                                               int withdrawThreads, String withdrawAmount,
                                               String userId) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(depositThreads + withdrawThreads);
        CountDownLatch gate = new CountDownLatch(1);
        for (int i = 0; i < depositThreads; i++) {
            final String id = UUID.randomUUID().toString();
            executor.submit(() -> {
                try {
                    gate.await();
                    mockMvc.perform(post("/deposit")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(Map.of(
                                            "userId", userId, "amount", new BigDecimal(depositAmount),
                                            "idempotencyId", id))))
                            .andReturn();
                } catch (Exception ignored) {}
            });
        }
        for (int i = 0; i < withdrawThreads; i++) {
            final String id = UUID.randomUUID().toString();
            executor.submit(() -> {
                try {
                    gate.await();
                    mockMvc.perform(post("/withdraw")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(Map.of(
                                            "userId", userId, "amount", new BigDecimal(withdrawAmount),
                                            "idempotencyId", id))))
                            .andReturn();
                } catch (Exception ignored) {}
            });
        }
        gate.countDown();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
    }

    @Quando("{int} threads transferem {string} de {string} para {string} e {int} threads transferem {string} de {string} para {string} concorrentemente")
    public void threadsBidirecionais(int t1, String a1, String from1, String to1,
                                     int t2, String a2, String from2, String to2) throws Exception {
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

    @Quando("todas as chaves do Redis são apagadas")
    public void todasChavesRedisApagadas() {
        Set<String> keys = redisTemplate.keys("*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // ── Assertions — HTTP Status ──────────────────────────────────────────────

    @Entao("a resposta deve retornar status {int}")
    public void respostaDeveRetornarStatus(int status) {
        assertThat(lastResult.getResponse().getStatus()).isEqualTo(status);
    }

    @Entao("a resposta deve retornar status 4xx")
    public void respostaDeveRetornarStatus4xx() {
        assertThat(lastResult.getResponse().getStatus()).isBetween(400, 499);
    }

    @Entao("a resposta deve conter um {string}")
    public void respostaDeveConterCampo(String field) throws Exception {
        String body = lastResult.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(body).contains(field);
    }

    @Entao("a resposta deve conter userId {string}")
    public void respostaDeveConterUserId(String userId) throws Exception {
        String body = lastResult.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(body).contains(userId);
    }

    @Entao("a resposta deve conter balance {string}")
    public void respostaDeveConterBalance(String balance) throws Exception {
        String body = lastResult.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(body).contains(balance);
    }

    // ── Assertions — Saldo ────────────────────────────────────────────────────

    @Entao("o saldo da carteira de {string} deve ser {string}")
    public void saldoDeveSerExato(String userId, String expected) {
        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = ?", BigDecimal.class, userId);
        assertThat(balance).isEqualByComparingTo(expected);
    }

    @Entao("o saldo da carteira de {string} deve ser {string} em até {int} segundos")
    public void saldoDeveSerEmAte(String userId, String expected, int seconds) throws InterruptedException {
        waitForExactBalance(userId, new BigDecimal(expected), (long) seconds * 1000);
    }

    @Entao("o saldo da carteira de {string} deve ser maior ou igual a {string}")
    public void saldoDeveSerMaiorOuIgual(String userId, String minValue) {
        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = ?", BigDecimal.class, userId);
        assertThat(balance).isGreaterThanOrEqualTo(new BigDecimal(minValue));
    }

    @Entao("após aguardar 3 segundos o saldo da carteira de {string} deve permanecer {string}")
    public void aguardaESaldoPermanece(String userId, String expected) throws InterruptedException {
        Thread.sleep(3000);
        saldoDeveSerExato(userId, expected);
    }

    @Entao("a soma dos saldos de {string} e {string} deve ser {string}")
    public void somaDeSaldosDeveSer(String userA, String userB, String expected) {
        BigDecimal balA = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = ?", BigDecimal.class, userA);
        BigDecimal balB = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = ?", BigDecimal.class, userB);
        assertThat(balA.add(balB)).isEqualByComparingTo(expected);
    }

    @Entao("a soma total dos saldos de todos os participantes deve ser {string}")
    public void somaTotalDeveSerDezMil(String expected) {
        BigDecimal total = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(balance), 0) FROM wallets", BigDecimal.class);
        assertThat(total).isEqualByComparingTo(expected);
    }

    // ── Assertions — Banco de dados ───────────────────────────────────────────

    @Entao("deve existir {int} carteira no banco para o usuário {string}")
    public void deveExistirCarteiraNoBanco(int count, String userId) {
        Integer actual = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wallets WHERE user_id = ?", Integer.class, userId);
        assertThat(actual).isEqualTo(count);
    }

    @Entao("deve existir apenas {int} carteira no banco para o usuário {string}")
    public void deveExistirApenasCarteiraNoBanco(int count, String userId) {
        deveExistirCarteiraNoBanco(count, userId);
    }

    @Entao("deve existir {int} transação do tipo {string} para o usuário {string}")
    public void deveExistirTransacaoDoTipo(int count, String type, String userId) {
        Integer actual = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = ?) AND type = ?",
                Integer.class, userId, type);
        assertThat(actual).isEqualTo(count);
    }

    @Entao("deve existir {int} transação do tipo {string} para o usuário {string} em até {int} segundos")
    public void deveExistirTransacaoEmAte(int count, String type, String userId, int seconds) throws InterruptedException {
        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = '" + userId + "') AND type = '" + type + "'",
                count, (long) seconds * 1000);
    }

    @Entao("deve existir {int} transações do tipo {string} para o usuário {string} em até {int} segundos")
    public void deveExistirTransacoesDoTipoEmAte(int count, String type, String userId, int seconds) throws InterruptedException {
        deveExistirTransacaoEmAte(count, type, userId, seconds);
    }

    @Entao("deve existir {int} transação para o usuário {string} em até {int} segundos")
    public void deveExistirTransacaoParaUsuarioEmAte(int count, String userId, int seconds) throws InterruptedException {
        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = '" + userId + "')",
                count, (long) seconds * 1000);
    }

    @Entao("deve existir {int} transações para o usuário {string} em até {int} segundos")
    public void deveExistirTransacoesParaUsuarioEmAte(int count, String userId, int seconds) throws InterruptedException {
        deveExistirTransacaoParaUsuarioEmAte(count, userId, seconds);
    }

    @Entao("deve existir {int} transações vinculadas à transferência em até {int} segundos")
    public void deveExistirTransacoesVinculadasEmAte(int count, int seconds) throws InterruptedException, UnsupportedEncodingException, com.fasterxml.jackson.core.JsonProcessingException {
        Map<?, ?> response = objectMapper.readValue(
                lastResult.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8), Map.class);
        String trackId = response.get("transactionId").toString();
        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE transaction_track_id = '" + trackId + "'",
                count, (long) seconds * 1000);
    }

    @Entao("não deve existir nenhuma transação vinculada à transferência")
    public void naoDeveExistirTransacaoVinculada() throws InterruptedException, UnsupportedEncodingException, com.fasterxml.jackson.core.JsonProcessingException {
        Map<?, ?> response = objectMapper.readValue(
                lastResult.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8), Map.class);
        String trackId = response.get("transactionId").toString();
        Thread.sleep(3000);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE transaction_track_id = ?",
                Integer.class, trackId);
        assertThat(count).isZero();
    }

    @Entao("deve existir exatamente {int} transação do tipo {string} para o usuário {string}")
    public void deveExistirExatamenteTransacaoDoTipo(int count, String type, String userId) {
        deveExistirTransacaoDoTipo(count, type, userId);
    }

    @Entao("após aguardar 3 segundos não deve existir nenhuma transação no banco")
    public void naoDeveExistirTransacaoNoBanco() throws InterruptedException {
        Thread.sleep(3000);
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transactions", Integer.class);
        assertThat(count).isZero();
    }

    @Entao("após aguardar 3 segundos não deve existir nenhuma transação do tipo {string} para o usuário {string}")
    public void naoDeveExistirTransacaoDoTipoParaUsuario(String type, String userId) throws InterruptedException {
        Thread.sleep(3000);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = ?) AND type = ?",
                Integer.class, userId, type);
        assertThat(count).isZero();
    }

    @Entao("após aguardar 3 segundos não deve existir nenhuma transferência no banco")
    public void naoDeveExistirTransferenciaNoBanco() throws InterruptedException {
        Thread.sleep(3000);
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transfers", Integer.class);
        assertThat(count).isZero();
    }

    @Entao("deve existir {int} registro de transferência no banco em até {int} segundos")
    public void deveExistirRegistroTransferenciaEmAte(int count, int seconds) throws InterruptedException {
        waitForCondition("SELECT COUNT(*) FROM transfers", count, (long) seconds * 1000);
    }

    @Entao("deve existir {int} registros de transferência no banco em até {int} segundos")
    public void deveExistirRegistrosTransferenciaEmAte(int count, int seconds) throws InterruptedException {
        deveExistirRegistroTransferenciaEmAte(count, seconds);
    }

    @Entao("o registro de transferência deve ter debit_transaction_id e credit_transaction_id distintos")
    public void registroTransferenciaDeveTermeDebitECredit() {
        Map<String, Object> transfer = jdbcTemplate.queryForMap(
                "SELECT debit_transaction_id, credit_transaction_id FROM transfers");
        assertThat(transfer.get("debit_transaction_id")).isNotNull();
        assertThat(transfer.get("credit_transaction_id")).isNotNull();
        assertThat(transfer.get("debit_transaction_id"))
                .isNotEqualTo(transfer.get("credit_transaction_id"));
    }

    // ── Assertions — Snapshots de Saldo ──────────────────────────────────────

    @Entao("o saldo antes da transação de {string} deve ser {string}")
    public void saldoAntesTransacaoDeveSer(String userId, String expected) {
        Map<String, Object> tx = jdbcTemplate.queryForMap(
                "SELECT balance_before_transaction FROM transactions " +
                "WHERE wallet_id = (SELECT id FROM wallets WHERE user_id = ?) LIMIT 1",
                userId);
        assertThat((BigDecimal) tx.get("balance_before_transaction")).isEqualByComparingTo(expected);
    }

    @Entao("o saldo depois da transação de {string} deve ser {string}")
    public void saldoDepoisTransacaoDeveSer(String userId, String expected) {
        Map<String, Object> tx = jdbcTemplate.queryForMap(
                "SELECT balance_after_transaction FROM transactions " +
                "WHERE wallet_id = (SELECT id FROM wallets WHERE user_id = ?) LIMIT 1",
                userId);
        assertThat((BigDecimal) tx.get("balance_after_transaction")).isEqualByComparingTo(expected);
    }

    @Entao("o saldo antes da transação de saque de {string} deve ser {string}")
    public void saldoAntesTransacaoSaqueDeveSer(String userId, String expected) {
        Map<String, Object> tx = jdbcTemplate.queryForMap(
                "SELECT balance_before_transaction FROM transactions " +
                "WHERE wallet_id = (SELECT id FROM wallets WHERE user_id = ?) AND type = 'WITHDRAW'",
                userId);
        assertThat((BigDecimal) tx.get("balance_before_transaction")).isEqualByComparingTo(expected);
    }

    @Entao("o saldo depois da transação de saque de {string} deve ser {string}")
    public void saldoDepoisTransacaoSaqueDeveSer(String userId, String expected) {
        Map<String, Object> tx = jdbcTemplate.queryForMap(
                "SELECT balance_after_transaction FROM transactions " +
                "WHERE wallet_id = (SELECT id FROM wallets WHERE user_id = ?) AND type = 'WITHDRAW'",
                userId);
        assertThat((BigDecimal) tx.get("balance_after_transaction")).isEqualByComparingTo(expected);
    }

    // ── Assertions — Tempo de Resposta ────────────────────────────────────────

    @Entao("o tempo de resposta deve ser inferior a {int} milissegundos")
    public void tempoRespostaInferiorA(int ms) {
        long elapsed = System.currentTimeMillis() - requestStartTime;
        assertThat(elapsed).isLessThan(ms);
    }

    // ── Assertions — Histórico ────────────────────────────────────────────────

    @Entao("a resposta deve conter uma lista com {int} transação")
    public void respostaDeveConterListaComTransacao(int count) throws Exception {
        String body = lastResult.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        List<?> list = objectMapper.readValue(body, List.class);
        assertThat(list).hasSize(count);
    }

    @Entao("a resposta deve conter uma lista com {int} transações")
    public void respostaDeveConterListaComTransacoes(int count) throws Exception {
        respostaDeveConterListaComTransacao(count);
    }

    @Entao("a resposta deve conter uma lista vazia")
    public void respostaDeveConterListaVazia() throws Exception {
        String body = lastResult.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        List<?> list = objectMapper.readValue(body, List.class);
        assertThat(list).isEmpty();
    }

    @Entao("a primeira transação deve conter balanceBeforeTransaction igual a {string}")
    public void primeiraTransacaoDeveConterBalanceBefore(String expected) throws Exception {
        String body = lastResult.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        List<Map<?, ?>> list = objectMapper.readValue(body, List.class);
        assertThat(list.get(0).get("balanceBeforeTransaction").toString()).isEqualTo(expected);
    }

    @Entao("a primeira transação deve conter balanceAfterTransaction igual a {string}")
    public void primeiraTransacaoDeveConterBalanceAfter(String expected) throws Exception {
        String body = lastResult.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        List<Map<?, ?>> list = objectMapper.readValue(body, List.class);
        assertThat(list.get(0).get("balanceAfterTransaction").toString()).isEqualTo(expected);
    }

    // ── Assertions — Event Sourcing ───────────────────────────────────────────

    @Entao("deve existir exatamente {int} evento do tipo {string} para o agregado {string}")
    public void deveExistirEventoParaAgregado(int count, String eventType, String aggregateId) {
        Integer actual = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type = ? AND aggregate_id = ?",
                Integer.class, eventType, aggregateId);
        assertThat(actual).isEqualTo(count);
    }

    @Entao("deve existir exatamente {int} evento do tipo {string} para o agregado {string} em até {int} segundos")
    public void deveExistirEventoParaAgregadoEmAte(int count, String eventType, String aggregateId, int seconds)
            throws InterruptedException {
        waitForCondition(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type = '" + eventType +
                "' AND aggregate_id = '" + aggregateId + "'",
                count, (long) seconds * 1000);
        deveExistirEventoParaAgregado(count, eventType, aggregateId);
    }

    @Entao("não deve existir nenhum evento do tipo {string} para o agregado {string}")
    public void naoDeveExistirEventoParaAgregado(String eventType, String aggregateId) {
        Integer actual = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type = ? AND aggregate_id = ?",
                Integer.class, eventType, aggregateId);
        assertThat(actual).isZero();
    }

    @Entao("deve existir exatamente {int} evento do tipo {string} para o agregado de transferência em até {int} segundos")
    public void deveExistirEventoParaAgregadoTransferenciaEmAte(int count, String eventType, int seconds)
            throws InterruptedException {
        waitForCondition(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type = '" + eventType +
                "' AND aggregate_type = 'TRANSFER' AND aggregate_id = '" + capturedTransferAggregateId + "'",
                count, (long) seconds * 1000);
        Integer actual = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type = ? AND aggregate_type = 'TRANSFER' AND aggregate_id = ?",
                Integer.class, eventType, capturedTransferAggregateId);
        assertThat(actual).isEqualTo(count);
    }

    @Entao("não deve existir nenhum evento de retry para o agregado de transferência")
    public void naoDeveExistirEventoRetryParaTransferencia() {
        Integer actual = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type = 'TRANSACTION_RETRY_ATTEMPTED' " +
                "AND aggregate_id = ?", Integer.class, capturedTransferAggregateId);
        assertThat(actual).isZero();
    }

    @Entao("não deve existir nenhum evento dos tipos {string}, {string} ou {string} para {string}")
    public void naoDeveExistirEventosDostipos(String t1, String t2, String t3, String aggregateId) {
        Integer actual = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type IN (?, ?, ?) AND aggregate_id = ?",
                Integer.class, t1, t2, t3, aggregateId);
        assertThat(actual).isZero();
    }

    @Entao("o total de eventos para o agregado {string} deve ser exatamente {int}")
    public void totalEventosParaAgregadoDeveSer(String aggregateId, int count) {
        Integer actual = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wallet_events WHERE aggregate_id = ?", Integer.class, aggregateId);
        assertThat(actual).isEqualTo(count);
    }

    @Entao("o payload do evento {string} de {string} deve conter balanceBefore igual a {string}")
    public void payloadDeveConterBalanceBefore(String eventType, String aggregateId, String expected) {
        String value = jdbcTemplate.queryForObject(
                "SELECT payload->>'balanceBefore' FROM wallet_events WHERE event_type = ? AND aggregate_id = ?",
                String.class, eventType, aggregateId);
        assertThat(value).isEqualTo(expected);
    }

    @Entao("o payload do evento {string} de {string} deve conter balanceAfter igual a {string}")
    public void payloadDeveConterBalanceAfter(String eventType, String aggregateId, String expected) {
        String value = jdbcTemplate.queryForObject(
                "SELECT payload->>'balanceAfter' FROM wallet_events WHERE event_type = ? AND aggregate_id = ?",
                String.class, eventType, aggregateId);
        assertThat(value).isEqualTo(expected);
    }

    @Entao("o payload do evento {string} do agregado de transferência deve conter:")
    public void payloadTransferenciaDeveConter(String eventType, io.cucumber.datatable.DataTable table) {
        Map<String, Object> fields = jdbcTemplate.queryForMap(
                "SELECT payload->>'fromBalanceBefore' AS fbb, payload->>'fromBalanceAfter' AS fba," +
                "       payload->>'toBalanceBefore' AS tbb, payload->>'toBalanceAfter' AS tba " +
                "FROM wallet_events WHERE event_type = ? AND aggregate_type = 'TRANSFER' AND aggregate_id = ?",
                eventType, capturedTransferAggregateId);

        table.asMaps().forEach(row -> {
            String campo = row.get("campo");
            String valor = row.get("valor");
            switch (campo) {
                case "fromBalanceBefore" -> assertThat(fields.get("fbb").toString()).isEqualTo(valor);
                case "fromBalanceAfter"  -> assertThat(fields.get("fba").toString()).isEqualTo(valor);
                case "toBalanceBefore"   -> assertThat(fields.get("tbb").toString()).isEqualTo(valor);
                case "toBalanceAfter"    -> assertThat(fields.get("tba").toString()).isEqualTo(valor);
            }
        });
    }

    @Entao("o payload do evento {string} de {string} deve conter reason com {string}")
    public void payloadDeveConterReason(String eventType, String aggregateId, String expectedSubstring) {
        String value = jdbcTemplate.queryForObject(
                "SELECT payload->>'reason' FROM wallet_events WHERE event_type = ? AND aggregate_id = ?",
                String.class, eventType, aggregateId);
        assertThat(value).contains(expectedSubstring);
    }

    @Entao("o payload do evento {string} de {string} deve conter retryCount igual a {string}")
    public void payloadDeveConterRetryCount(String eventType, String aggregateId, String expected) {
        String value = jdbcTemplate.queryForObject(
                "SELECT payload->>'retryCount' FROM wallet_events WHERE event_type = ? AND aggregate_id = ?",
                String.class, eventType, aggregateId);
        assertThat(value).isEqualTo(expected);
    }

    @Entao("o payload do evento {string} de {string} deve conter transactionType igual a {string}")
    public void payloadDeveConterTransactionType(String eventType, String aggregateId, String expected) {
        String value = jdbcTemplate.queryForObject(
                "SELECT payload->>'transactionType' FROM wallet_events WHERE event_type = ? AND aggregate_id = ?",
                String.class, eventType, aggregateId);
        assertThat(value).isEqualTo(expected);
    }

    // ── Helpers internos ──────────────────────────────────────────────────────

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
            bodies.add(Map.of(
                    "userId", userId,
                    "amount", new BigDecimal(amount),
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
