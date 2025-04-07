package br.com.wallet.project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/redis")
public class RedisController {

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @GetMapping("/test")
    public String testRedisConnection() {
        try {
            redisConnectionFactory.getConnection().ping();
            return "Conectado ao Redis com sucesso!";
        } catch (Exception e) {
            return "Erro ao conectar no Redis: " + e.getMessage();
        }
    }
}
