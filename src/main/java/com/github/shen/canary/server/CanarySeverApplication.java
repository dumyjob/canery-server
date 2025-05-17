package com.github.shen.canary.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Repository;

@SpringBootApplication
@MapperScan(basePackages = "com.github.shen.canary.server.*.dao"
		, annotationClass = Repository.class)
public class CanarySeverApplication {

	public static void main(String[] args) {
		SpringApplication.run(CanarySeverApplication.class, args);
	}

}
