package br.com.wallet.project.functional.cucumber.runner;

import io.cucumber.junit.platform.engine.Cucumber;

/**
 * Ponto de entrada do Cucumber para o JUnit Platform.
 *
 * Usa @Cucumber (do cucumber-junit-platform-engine) em vez de @Suite +
 * @IncludeEngines, evitando a dependência junit-platform-suite.
 *
 * As configurações de glue e plugin são lidas de
 * src/test/resources/cucumber.properties.
 */
@Cucumber
public class CucumberTest {}
