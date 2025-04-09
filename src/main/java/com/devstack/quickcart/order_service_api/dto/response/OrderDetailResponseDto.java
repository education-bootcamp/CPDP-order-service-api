package com.devstack.quickcart.order_service_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderDetailResponseDto {
    private String detailId;
    private String productId;
    private int qty;
    private double unitPrice;
    private double discount;
}
