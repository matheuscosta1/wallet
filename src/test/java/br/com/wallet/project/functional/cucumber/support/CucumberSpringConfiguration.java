package br.com.wallet.project.functional.cucumber.support;

import br.com.wallet.project.functional.BaseFunctionalTest;
import io.cucumber.spring.CucumberContextConfiguration;

/**
 * Configuração do contexto Spring para o Cucumber.
 *
 * Estende BaseFunctionalTest para herdar:
 *  - @SpringBootTest com porta aleatória
 *  - @AutoConfigureMockMvc
 *  - @DynamicPropertySource com ContainerManager
 *  - limpeza de banco, Redis e Kafka via @BeforeEach
 */
@CucumberContextConfiguration
public class CucumberSpringConfiguration extends BaseFunctionalTest {
}
