package com.dis.workshopticketing.paymentservice.service;

import com.dis.workshopticketing.paymentservice.client.BookingClient;
import com.dis.workshopticketing.paymentservice.dto.CreatePaymentRequest;
import com.dis.workshopticketing.paymentservice.exception.PaymentProcessingException;
import com.dis.workshopticketing.paymentservice.exception.ResourceNotFoundException;
import com.dis.workshopticketing.paymentservice.model.Payment;
import com.dis.workshopticketing.paymentservice.model.PaymentStatus;
import com.dis.workshopticketing.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingClient bookingClient;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, bookingClient);
    }

    @Test
    void createSuccessfulPaymentConfirmsBookingAndStoresSucceededPayment() {
        List<PaymentStatus> savedStatuses = new ArrayList<>();
        stubSaveAndFlush(savedStatuses);

        var response = paymentService.create(7L, new CreatePaymentRequest(
                10L,
                new BigDecimal("50.00"),
                "eur",
                false
        ));

        verify(bookingClient).confirmPayment(10L);
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(7L);
        assertThat(response.bookingId()).isEqualTo(10L);
        assertThat(response.amount()).isEqualByComparingTo("50.00");
        assertThat(response.currency()).isEqualTo("EUR");
        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(savedStatuses).containsExactly(PaymentStatus.INITIATED, PaymentStatus.SUCCEEDED);
    }

    @Test
    void createFailedPaymentNotifiesBookingAndStoresFailedPayment() {
        List<PaymentStatus> savedStatuses = new ArrayList<>();
        stubSaveAndFlush(savedStatuses);

        var response = paymentService.create(7L, new CreatePaymentRequest(
                10L,
                new BigDecimal("50.00"),
                "EUR",
                true
        ));

        verify(bookingClient).failPayment(10L);
        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(savedStatuses).containsExactly(PaymentStatus.INITIATED, PaymentStatus.FAILED);
    }

    @Test
    void createThrowsPaymentProcessingExceptionWhenBookingUpdateFails() {
        stubSaveAndFlush(new ArrayList<>());
        when(bookingClient.confirmPayment(10L)).thenThrow(new RuntimeException("booking-service unavailable"));

        assertThatThrownBy(() -> paymentService.create(7L, new CreatePaymentRequest(
                10L,
                new BigDecimal("50.00"),
                "EUR",
                false
        )))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessage("Payment could not update booking outcome: 1");
    }

    @Test
    void getDoesNotAllowUserToReadAnotherUsersPayment() {
        when(paymentRepository.findByIdAndUserId(55L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.get(7L, 55L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Payment not found: 55");
    }

    private void stubSaveAndFlush(List<PaymentStatus> savedStatuses) {
        AtomicLong nextId = new AtomicLong(1L);
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            savedStatuses.add(payment.getStatus());
            if (payment.getId() == null) {
                payment.setId(nextId.getAndIncrement());
            }
            return payment;
        });
    }
}
