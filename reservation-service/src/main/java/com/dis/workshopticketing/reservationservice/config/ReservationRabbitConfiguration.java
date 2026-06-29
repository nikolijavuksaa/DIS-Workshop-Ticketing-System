package com.dis.workshopticketing.reservationservice.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReservationRabbitConfiguration {

    @Bean
    TopicExchange reservationEventsExchange(
            @Value("${reservation.events.exchange:reservation.events}") String exchangeName
    ) {
        return new TopicExchange(exchangeName);
    }
}
