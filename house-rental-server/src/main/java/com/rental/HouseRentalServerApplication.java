package com.rental;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.rental.mapper")
@SpringBootApplication
public class HouseRentalServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(HouseRentalServerApplication.class, args);
    }
}