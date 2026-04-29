package com.medsetu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MedsetuApplication {
    public static void main(String[] args) {
        SpringApplication.run(MedsetuApplication.class, args);
    }
}
