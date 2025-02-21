package br.com.wallet.project.service.functional;

import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;

@Testcontainers
public interface TestContainerSetup {
    default DockerComposeContainer<?> getContainer() {
        DockerComposeContainer<?> container = new DockerComposeContainer<> (
                new File("src/main/resources/compose/docker-compose.yml"))
                .withExposedService("kafka_1", 9092, Wait.forListeningPort())
                .withExposedService("postgres_1", 5432, Wait.forListeningPort())
                .withExposedService("zookeeper_1", 2181, Wait.forListeningPort());
        container.start();
        return container;
    }
}
