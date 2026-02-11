package br.com.wallet.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@SpringBootApplication
@EnableJpaRepositories("br.com.wallet.project.*")
@EnableRedisRepositories
@ComponentScan(basePackages = {"br.com.wallet.project.*"})
@EntityScan("br.com.wallet.project.*")
public class WalletApplication {
  public static void main(String[] args) {
    SpringApplication.run(WalletApplication.class, args);
  }
}
