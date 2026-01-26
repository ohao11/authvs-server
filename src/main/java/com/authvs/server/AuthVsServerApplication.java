package com.authvs.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.authvs.server.mapper")
public class AuthVsServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthVsServerApplication.class, args);
    }

}
