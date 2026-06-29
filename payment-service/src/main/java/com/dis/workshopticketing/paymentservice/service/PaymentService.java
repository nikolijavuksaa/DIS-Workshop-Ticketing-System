package com.dis.workshopticketing.paymentservice.service;

import com.dis.workshopticketing.paymentservice.client.BookingClient;
import com.dis.workshopticketing.paymentservice.dto.CreatePaymentRequest;
import com.dis.workshopticketing.paymentservice.dto.PaymentResponse;
import com.dis.workshopticketing.paymentservice.exception.PaymentProcessingException;
import com.dis.workshopticketing.paymentservice.exception.ResourceNotFoundException;
import com.dis.workshopticketing.paymentservice.model.Payment;
import com.dis.workshopticketing.paymentservice.model.PaymentStatus;
import com.dis.workshopticketing.paymentservice.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingClient bookingClient;

    public PaymentService(PaymentRepository paymentRepository, BookingClient bookingClient) {
        this.paymentRepository = paymentRepository;
        this.bookingClient = bookingClient;
    }

    public PaymentResponse create(Long userId, CreatePaymentRequest request) {
        Payment payment = Payment.builder()
                .bookingId(request.bookingId())
                .userId(userId)
                .amount(request.amount())
                .currency(request.currency().toUpperCase())
                .status(PaymentStatus.INITIATED)
                .build();

        Payment savedPayment = paymentRepository.saveAndFlush(payment);

        try {
            if (request.shouldSimulateFailure()) {
                bookingClient.failPayment(savedPayment.getBookingId());
                savedPayment.setStatus(PaymentStatus.FAILED);
            } else {
                bookingClient.confirmPayment(savedPayment.getBookingId());
                savedPayment.setStatus(PaymentStatus.SUCCEEDED);
            }

            return PaymentResponse.from(paymentRepository.saveAndFlush(savedPayment));
        } catch (RuntimeException exception) {
            throw new PaymentProcessingException(savedPayment.getId(), exception);
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(Long userId, Long id) {
        return PaymentResponse.from(findOwnedPayment(userId, id));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAll(Long userId) {
        return paymentRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(PaymentResponse::from)
                .toList();
    }

    private Payment findOwnedPayment(Long userId, Long id) {
        return paymentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
    }
}
