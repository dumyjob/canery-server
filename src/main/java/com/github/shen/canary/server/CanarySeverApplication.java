package com.github.shen.canary.server;


import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Repository;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan(basePackages = {"com.github.shen.canary.server.dao", "com.github.shen.canary.server.*.dao"}
		, annotationClass = Repository.class)
public class CanarySeverApplication {

	public static void main(String[] args) {
		SpringApplication.run(CanarySeverApplication.class, args);
	}

}
