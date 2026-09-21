package com.eximee.los;

import org.eximeebpms.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableProcessApplication
public class LosApplication {

    public static void main(String[] args) {
        SpringApplication.run(LosApplication.class, args);
    }
}
