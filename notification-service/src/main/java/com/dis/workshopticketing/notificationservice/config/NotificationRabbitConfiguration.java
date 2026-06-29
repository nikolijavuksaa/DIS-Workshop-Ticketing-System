package com.dis.workshopticketing.notificationservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationRabbitConfiguration {

    public static final String RESERVATION_HELD = "reservation.held";
    public static final String RESERVATION_WAITLISTED = "reservation.waitlisted";
    public static final String RESERVATION_CONFIRMED = "reservation.confirmed";
    public static final String RESERVATION_EXPIRED = "reservation.expired";
    public static final String WAITLIST_PROMOTED = "waitlist.promoted";

    @Bean
    TopicExchange reservationEventsExchange(
            @Value("${reservation.events.exchange:reservation.events}") String exchangeName
    ) {
        return new TopicExchange(exchangeName);
    }

    @Bean
    Queue notificationReservationEventsQueue(
            @Value("${notification.reservation-events.queue:notification.reservation-events}") String queueName
    ) {
        return new Queue(queueName, true);
    }

    @Bean
    Declarables notificationReservationEventBindings(Queue notificationReservationEventsQueue, TopicExchange reservationEventsExchange) {
        return new Declarables(
                bind(notificationReservationEventsQueue, reservationEventsExchange, RESERVATION_HELD),
                bind(notificationReservationEventsQueue, reservationEventsExchange, RESERVATION_WAITLISTED),
                bind(notificationReservationEventsQueue, reservationEventsExchange, RESERVATION_CONFIRMED),
                bind(notificationReservationEventsQueue, reservationEventsExchange, RESERVATION_EXPIRED),
                bind(notificationReservationEventsQueue, reservationEventsExchange, WAITLIST_PROMOTED)
        );
    }

    @Bean
    MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    private Binding bind(Queue queue, TopicExchange exchange, String routingKey) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }
}
