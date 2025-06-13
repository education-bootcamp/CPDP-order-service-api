package com.devstack.quickcart.order_service_api.service.impl;

import com.devstack.quickcart.order_service_api.dto.request.PaymentRequestDto;
import com.devstack.quickcart.order_service_api.dto.response.PaymentResponseDto;
import com.devstack.quickcart.order_service_api.service.PaymentService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }


    @Override
    public PaymentResponseDto createPaymentIntent(PaymentRequestDto paymentRequest, double amount) {
        try {
            // Convert amount to cents (Stripe works with smallest currency unit)
            long amountInCents = Math.round(amount * 100);

            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(paymentRequest.getCurrency() != null ? paymentRequest.getCurrency() : "usd")
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    );

            // Add optional parameters
            if (paymentRequest.getReceiptEmail() != null) {
                paramsBuilder.setReceiptEmail(paymentRequest.getReceiptEmail());
            }

            if (paymentRequest.getDescription() != null) {
                paramsBuilder.setDescription(paymentRequest.getDescription());
            }

            if (paymentRequest.getPaymentMethodId() != null) {
                paramsBuilder.setPaymentMethod(paymentRequest.getPaymentMethodId());
                if (paymentRequest.isConfirmPayment()) {
                    paramsBuilder.setConfirm(true);
                }
            }

            PaymentIntent paymentIntent = PaymentIntent.create(paramsBuilder.build());

            return PaymentResponseDto.builder()
                    .paymentIntentId(paymentIntent.getId())
                    .clientSecret(paymentIntent.getClientSecret())
                    .status(paymentIntent.getStatus())
                    .amount(paymentIntent.getAmount())
                    .currency(paymentIntent.getCurrency())
                    .paymentMethodId(paymentIntent.getPaymentMethod())
                    .build();

        } catch (StripeException e) {
            throw new RuntimeException("Payment processing failed: " + e.getMessage());
        }
    }

    @Override
    public PaymentResponseDto confirmPayment(String paymentIntentId) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            paymentIntent = paymentIntent.confirm();

            return PaymentResponseDto.builder()
                    .paymentIntentId(paymentIntent.getId())
                    .status(paymentIntent.getStatus())
                    .amount(paymentIntent.getAmount())
                    .currency(paymentIntent.getCurrency())
                    .receiptUrl("")
                    .build();

        } catch (StripeException e) {
            throw new RuntimeException("Payment confirmation failed: " + e.getMessage());
        }
    }

    @Override
    public PaymentResponseDto cancelPayment(String paymentIntentId) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            paymentIntent = paymentIntent.cancel();

            return PaymentResponseDto.builder()
                    .paymentIntentId(paymentIntent.getId())
                    .status(paymentIntent.getStatus())
                    .build();

        } catch (StripeException e) {
            throw new RuntimeException("Payment cancellation failed: " + e.getMessage());
        }
    }

    @Override
    public PaymentResponseDto getPaymentStatus(String paymentIntentId) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

            return PaymentResponseDto.builder()
                    .paymentIntentId(paymentIntent.getId())
                    .status(paymentIntent.getStatus())
                    .amount(paymentIntent.getAmount())
                    .currency(paymentIntent.getCurrency())
                    .receiptUrl("")
                    .build();

        } catch (StripeException e) {
            throw new RuntimeException("Failed to retrieve payment status: " + e.getMessage());
        }
    }

    @Override
    public void handleWebhook(String payload, String signature) {
        try {
            Event event = Webhook.constructEvent(payload, signature, webhookSecret);

            switch (event.getType()) {
                case "payment_intent.succeeded":
                    handlePaymentSucceeded(event);
                    break;
                case "payment_intent.payment_failed":
                    handlePaymentFailed(event);
                    break;
                case "payment_intent.canceled":
                    handlePaymentCanceled(event);
                    break;
                default:

            }

        } catch (Exception e) {
            throw new RuntimeException("Webhook processing failed: " + e.getMessage());
        }
    }

    private void handlePaymentSucceeded(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
        if (paymentIntent != null) {
            // Here you can update order status, send confirmation emails, etc.
        }
    }

    private void handlePaymentFailed(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
        if (paymentIntent != null) {
            // Here you can update order status, notify customer, etc.
        }
    }

    private void handlePaymentCanceled(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
        if (paymentIntent != null) {
            // Here you can update order status, handle cancellation logic, etc.
        }
    }

}
