package br.com.wallet.project.functional;

import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * Registra as properties dinâmicas do Spring apontando para os serviços
 * corretos — seja Docker local ou containers gerenciados pelo Testcontainers.
 *
 * <p>Como {@code @DynamicPropertySource} só funciona em métodos estáticos de
 * classes concretas (não em interfaces), este utilitário expõe um método
 * estático que é chamado explicitamente a partir do {@code @DynamicPropertySource}
 * da {@link BaseFunctionalTest}.
 *
 * <p>Para adicionar novos serviços (ex.: Redis com porta dinâmica), inclua
 * os resolvers correspondentes no {@link ContainerManager} e registre a
 * property aqui.
 */
public final class TestPropertyOverride {

    private TestPropertyOverride() {}

    /**
     * Registra datasource, Kafka bootstrap-servers e demais properties
     * com os valores resolvidos pelo {@link ContainerManager}.
     *
     * @param registry registry injetado pelo framework via {@code @DynamicPropertySource}
     */
    public static void register(DynamicPropertyRegistry registry) {

        // ── PostgreSQL ────────────────────────────────────────────────────────
        registry.add("spring.datasource.url", () -> String.format(
            "jdbc:postgresql://%s:%d/wallet",
            ContainerManager.postgresHost(),
            ContainerManager.postgresPort()));

        // ── Kafka ─────────────────────────────────────────────────────────────
        registry.add("spring.kafka.bootstrap-servers", () -> String.format(
            "%s:%d",
            ContainerManager.kafkaHost(),
            ContainerManager.kafkaPort()));

        // ── Redis ─────────────────────────────────────────────────────────────
        // Redis não estava no docker-compose original, portanto mantemos
        // localhost:6379 como padrão. Adicione ao ContainerManager se quiser
        // tornar a porta dinâmica também.
    }
}
