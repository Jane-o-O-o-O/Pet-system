package com.example.petmgmt;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.example.petmgmt.mapper")
@EnableScheduling
public class PetMgmtApplication {
    public static void main(String[] args) {
        SpringApplication.run(PetMgmtApplication.class, args);
    }
}
