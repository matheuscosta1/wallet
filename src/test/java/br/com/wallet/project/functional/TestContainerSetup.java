package br.com.wallet.project.functional;

import java.io.IOException;
import java.net.Socket;

/**
 * Utilitário que verifica se os serviços de infraestrutura já estão
 * acessíveis localmente (Docker Engine do desenvolvedor).
 *
 * <p>Não possui mais a responsabilidade de subir containers — isso foi
 * delegado ao {@link ContainerManager}, que decide a estratégia em
 * tempo de inicialização dos testes.
 */
public interface TestContainerSetup {

    /**
     * Tenta abrir uma conexão TCP em {@code host:port}.
     *
     * @return {@code true} se o serviço estiver acessível
     */
    static boolean isReachable(String host, int port) {
        try (Socket s = new Socket(host, port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Retorna {@code true} se Postgres (:5432), Kafka (:9092) e
     * Zookeeper (:2181) estiverem todos acessíveis em localhost.
     */
    static boolean isLocalDockerRunning() {
        return isReachable("localhost", 5432)
            && isReachable("localhost", 9092)
            && isReachable("localhost", 2181);
    }
}
