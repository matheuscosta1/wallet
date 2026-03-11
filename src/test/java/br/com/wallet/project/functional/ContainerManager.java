package br.com.wallet.project.functional;

import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;

/**
 * Gerencia o ciclo de vida dos containers de teste em um único lugar.
 *
 * <p>Estratégia de decisão (avaliada uma única vez, no carregamento da classe):
 * <ol>
 *   <li>Se Postgres (:5432), Kafka (:9092) e Zookeeper (:2181) já estiverem
 *       acessíveis em localhost, assume-se que o desenvolvedor tem um Docker
 *       Engine rodando e <strong>nenhum container é iniciado</strong>.</li>
 *   <li>Caso contrário, os containers são levantados via {@code docker-compose}
 *       usando Testcontainers. O compose é compartilhado entre todos os testes
 *       (campo {@code static}) para evitar restart a cada classe de teste.</li>
 * </ol>
 *
 * <p>Para forçar o uso do Docker local sem detecção automática, exporte a
 * variável de ambiente antes de rodar os testes:
 * <pre>{@code
 *   USE_LOCAL_DOCKER=true mvn test
 * }</pre>
 */
public final class ContainerManager {

    /**
     * {@code true} quando os serviços já estão disponíveis localmente
     * (ou quando {@code USE_LOCAL_DOCKER=true} está definido no ambiente).
     */
    public static final boolean USE_LOCAL =
        "true".equalsIgnoreCase(System.getenv("USE_LOCAL_DOCKER"))
            || TestContainerSetup.isLocalDockerRunning();

    /**
     * Instância compartilhada do docker-compose. Será {@code null} quando
     * {@link #USE_LOCAL} for {@code true}.
     */
    public static final DockerComposeContainer<?> COMPOSE =
        USE_LOCAL ? null : createAndStartCompose();

    private ContainerManager() {}

    private static DockerComposeContainer<?> createAndStartCompose() {
        DockerComposeContainer<?> compose = new DockerComposeContainer<>(
                new File("src/main/resources/compose/docker-compose.yml"))
            .withExposedService("kafka_1",     9092, Wait.forListeningPort())
            .withExposedService("postgres_1",  5432, Wait.forListeningPort())
            .withExposedService("zookeeper_1", 2181, Wait.forListeningPort());
        compose.start();
        return compose;
    }

    // ── Resolvers de host/porta ───────────────────────────────────────────────

    public static String postgresHost() {
        return USE_LOCAL ? "localhost"
            : COMPOSE.getServiceHost("postgres_1", 5432);
    }

    public static int postgresPort() {
        return USE_LOCAL ? 5432
            : COMPOSE.getServicePort("postgres_1", 5432);
    }

    public static String kafkaHost() {
        return USE_LOCAL ? "localhost"
            : COMPOSE.getServiceHost("kafka_1", 9092);
    }

    public static int kafkaPort() {
        return USE_LOCAL ? 9092
            : COMPOSE.getServicePort("kafka_1", 9092);
    }

    public static String zookeeperHost() {
        return USE_LOCAL ? "localhost"
            : COMPOSE.getServiceHost("zookeeper_1", 2181);
    }

    public static int zookeeperPort() {
        return USE_LOCAL ? 2181
            : COMPOSE.getServicePort("zookeeper_1", 2181);
    }
}
