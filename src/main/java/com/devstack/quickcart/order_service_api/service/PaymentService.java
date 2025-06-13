package com.devstack.quickcart.order_service_api.service;

import com.devstack.quickcart.order_service_api.dto.request.PaymentRequestDto;
import com.devstack.quickcart.order_service_api.dto.response.PaymentResponseDto;

public interface PaymentService {
    PaymentResponseDto createPaymentIntent(PaymentRequestDto paymentRequest, double amount);
    PaymentResponseDto confirmPayment(String paymentIntentId);
    PaymentResponseDto cancelPayment(String paymentIntentId);
    PaymentResponseDto getPaymentStatus(String paymentIntentId);
    void handleWebhook(String payload, String signature);
}
