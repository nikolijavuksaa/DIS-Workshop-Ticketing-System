package com.dis.workshopticketing.workshopservice;

import org.springframework.boot.SpringApplication;

public class TestWorkshopServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(WorkshopServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
