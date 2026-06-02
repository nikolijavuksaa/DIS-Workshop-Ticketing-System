package com.dis.workshopticketing.bookingservice;

import org.springframework.boot.SpringApplication;

public class TestBookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(BookingServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
