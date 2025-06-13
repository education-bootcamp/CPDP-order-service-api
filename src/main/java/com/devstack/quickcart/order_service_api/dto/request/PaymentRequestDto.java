package com.devstack.quickcart.order_service_api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequestDto {
    private String paymentMethodId;
    private String currency;
    private String receiptEmail;
    private String description;
    private boolean confirmPayment;
}
