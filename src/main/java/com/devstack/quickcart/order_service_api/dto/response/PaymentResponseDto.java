package com.devstack.quickcart.order_service_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponseDto {
    private String paymentIntentId;
    private String clientSecret;
    private String status;
    private String paymentMethodId;
    private Long amount;
    private String currency;
    private String receiptUrl;
    private String failureReason;
}