package com.devstack.quickcart.order_service_api.service.impl;

import com.devstack.quickcart.order_service_api.dto.request.CustomerOrderRequestDto;
import com.devstack.quickcart.order_service_api.dto.request.OrderDetailRequestDto;
import com.devstack.quickcart.order_service_api.dto.request.PaymentRequestDto;
import com.devstack.quickcart.order_service_api.dto.response.CustomerOrderResponseDto;
import com.devstack.quickcart.order_service_api.dto.response.OrderDetailResponseDto;
import com.devstack.quickcart.order_service_api.dto.response.PaymentResponseDto;
import com.devstack.quickcart.order_service_api.dto.response.paginate.CustomerOrderPaginateDto;
import com.devstack.quickcart.order_service_api.entity.CustomerOrder;
import com.devstack.quickcart.order_service_api.entity.OrderDetail;
import com.devstack.quickcart.order_service_api.entity.OrderStatus;
import com.devstack.quickcart.order_service_api.exception.EntryNotFoundException;
import com.devstack.quickcart.order_service_api.repo.CustomerOrderRepo;
import com.devstack.quickcart.order_service_api.repo.OrderDetailRepo;
import com.devstack.quickcart.order_service_api.repo.OrderStatusRepo;
import com.devstack.quickcart.order_service_api.service.CustomerOrderService;
import com.devstack.quickcart.order_service_api.service.PaymentService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.hibernate.query.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerOrderServiceImpl implements CustomerOrderService {

    private final CustomerOrderRepo customerOrderRepo;
    private final OrderStatusRepo orderStatusRepo;
    private final OrderDetailRepo orderDetailRepo;
    private final JwtService jwtService;
    private final PaymentService paymentService;

    @Transactional
    @Override
    public PaymentResponseDto  createOrder(CustomerOrderRequestDto requestDto, String tokenHeader) {
        PaymentResponseDto paymentResponse;
        try {
            // Extract email from token and get user information
            String userId = getUserEmailFromToken(tokenHeader);

            // Validate order request
            validateCreateOrderRequest(requestDto);

            // Get PENDING order status
            OrderStatus orderStatus = orderStatusRepo.findByStatus("PENDING")
                    .orElseThrow(() -> new EntryNotFoundException("Order Status Not Found. Please contact admin to resolve this issue"));

            // Generate unique order ID with better format
            String orderId = generateOrderId();

            // Calculate total amount from order details for security
            double calculatedTotal = calculateOrderTotal(requestDto.getOrderDetails());

            paymentResponse = paymentService.createPaymentIntent(
                    new PaymentRequestDto("CARD", "USD", userId, "", true)
                    , calculatedTotal);

            // Create customer order
            CustomerOrder customerOrder = new CustomerOrder();
            customerOrder.setOrderId(orderId);
            customerOrder.setOrderDate(new Date());
            customerOrder.setRemark("");
            customerOrder.setIntentId(paymentResponse.getPaymentIntentId());
            customerOrder.setTotalAmount(calculatedTotal); // Use calculated total for security
            customerOrder.setUserId(userId);
            customerOrder.setOrderStatus(orderStatus);
            Set<OrderDetail> orderDetails = new HashSet<>();
            for (OrderDetailRequestDto detailDto : requestDto.getOrderDetails()) {
                OrderDetail orderDetail = OrderDetail.builder()
                        .detailId(UUID.randomUUID().toString()) // Add detail ID
                        .qty(detailDto.getQty())
                        .productId(detailDto.getProductId())
                        .customerOrder(customerOrder)
                        .discount(detailDto.getDiscount() != 0 ? detailDto.getDiscount() : 0)
                        .unitPrice(detailDto.getUnitPrice())
                        .build();
                orderDetails.add(orderDetail);
            }

            customerOrder.setProducts(orderDetails);
            customerOrderRepo.save(customerOrder);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create order: " + e.getMessage(), e);
        }

        return paymentResponse;
    }

    @Override
    public void confirmPaymentAndUpdateOrder(String paymentIntentId) {
        try {
            // Confirm payment with Stripe
            PaymentResponseDto paymentResponse = paymentService.confirmPayment(paymentIntentId);

            // Find order by payment intent ID (you'll need to modify your repository)
            CustomerOrder order = customerOrderRepo.getContainingIntentId(paymentIntentId)
                    .orElseThrow(() -> new EntryNotFoundException("Order not found for payment intent: " + paymentIntentId));

            // Update order status based on payment status
            String orderStatusName;
            switch (paymentResponse.getStatus()) {
                case "succeeded":
                    orderStatusName = "CONFIRMED";
                    break;
                case "requires_action":
                    orderStatusName = "PAYMENT_ACTION_REQUIRED";
                    break;
                case "processing":
                    orderStatusName = "PAYMENT_PROCESSING";
                    break;
                default:
                    orderStatusName = "PAYMENT_FAILED";
            }

            OrderStatus newStatus = orderStatusRepo.findByStatus(orderStatusName)
                    .orElseThrow(() -> new EntryNotFoundException("Order status not found: " + orderStatusName));

            order.setOrderStatus(newStatus);
            order.setRemark(order.getRemark() + " | Payment Status: " + paymentResponse.getStatus());

            customerOrderRepo.save(order);

        } catch (Exception e) {
            throw new RuntimeException("Failed to confirm payment and update order: " + e.getMessage(), e);
        }
    }


    // Helper method to extract user ID from token
    private String getUserEmailFromToken(String tokenHeader) {
        try {
            // Validate token header
            if (tokenHeader == null || tokenHeader.trim().isEmpty()) {
                throw new IllegalArgumentException("Authorization token is required");
            }
            // Extract email from JWT token
            return jwtService.getEmail(tokenHeader);

        } catch (Exception e) {
            throw new IllegalStateException("Invalid or expired token", e);
        }
    }
    // Validation method for create order request
    private void validateCreateOrderRequest(CustomerOrderRequestDto requestDto) {
        if (requestDto == null) {
            throw new IllegalArgumentException("Order request cannot be null");
        }

        if (requestDto.getOrderDetails() == null || requestDto.getOrderDetails().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        if (requestDto.getTotalAmount() == 0) {
            throw new IllegalArgumentException("Total amount must be greater than zero");
        }

        // Validate each order detail
        for (OrderDetailRequestDto detail : requestDto.getOrderDetails()) {
            validateOrderDetail(detail);
        }
    }

    // Validation method for order details
    private void validateOrderDetail(OrderDetailRequestDto detail) {
        if (detail.getProductId() == null || detail.getProductId().trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be empty");
        }

        if (detail.getQty() == 0 || detail.getQty() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if (detail.getUnitPrice() == 0) {
            throw new IllegalArgumentException("Unit price must be greater than zero");
        }

        if (detail.getDiscount() != 0) {
            throw new IllegalArgumentException("Discount cannot be negative");
        }
    }

    // Method to calculate total amount from order details
    private double calculateOrderTotal(List<OrderDetailRequestDto> orderDetails) {
        double cost = 0;
        for (OrderDetailRequestDto d : orderDetails) {
            cost += d.getUnitPrice() - d.getDiscount();
        }
        return cost;
    }

    // Method to generate readable order ID
    private String generateOrderId() {
        return "ORD-" + System.currentTimeMillis() + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }


    @Override
    public void updateOrder(CustomerOrderRequestDto requestDto, String orderId) {
        CustomerOrder customerOrder =
                customerOrderRepo.findById(orderId).orElseThrow(() -> new EntryNotFoundException(String.format("Order not found with %s", orderId)));
        customerOrder.setOrderDate(new Date());
        customerOrder.setTotalAmount(requestDto.getTotalAmount());
        customerOrderRepo.save(customerOrder);
    }

    @Override
    public void manageRemark(String remark, String orderId) {
        CustomerOrder customerOrder =
                customerOrderRepo.findById(orderId).orElseThrow(() -> new EntryNotFoundException(String.format("Order not found with %s", orderId)));
        customerOrder.setRemark(remark);
        customerOrderRepo.save(customerOrder);
    }

    @Override
    public void manageStatus(String status, String orderId) {
        CustomerOrder customerOrder =
                customerOrderRepo.findById(orderId).orElseThrow(() -> new RuntimeException(String.format("Order not found with %s", orderId)));
        OrderStatus orderStatus = orderStatusRepo.findByStatus(status).orElseThrow(() -> new EntryNotFoundException("Order Status Not Found. so you can't place an order please contact admin"));
        customerOrder.setOrderStatus(orderStatus);
        customerOrderRepo.save(customerOrder);
    }


    @Override
    public CustomerOrderResponseDto findOrderById(String orderId) {
        CustomerOrder customerOrder =
                customerOrderRepo.findById(orderId).orElseThrow(() -> new EntryNotFoundException(String.format("Order not found with %s", orderId)));
        return toCustomerOrderResponseDto(customerOrder);
    }

    @Override
    public void handleFailedPayment(String paymentIntentId, String failureReason) {
        try {
            // Find order by payment intent ID
            CustomerOrder order = customerOrderRepo.getContainingIntentId(paymentIntentId)
                    .orElseThrow(() -> new EntryNotFoundException("Order not found for payment intent: " + paymentIntentId));

            // Update order status to payment failed
            OrderStatus failedStatus = orderStatusRepo.findByStatus("PAYMENT_FAILED")
                    .orElseThrow(() -> new EntryNotFoundException("Payment failed status not found"));

            order.setOrderStatus(failedStatus);
            order.setRemark(order.getRemark() + " | Payment Failed: " + failureReason);

            customerOrderRepo.save(order);

        } catch (Exception e) {
            throw new RuntimeException("Failed to handle payment failure: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteById(String orderId) {
        CustomerOrder customerOrder =
                customerOrderRepo.findById(orderId).orElseThrow(() -> new EntryNotFoundException(String.format("Order not found with %s", orderId)));
        customerOrderRepo.delete(customerOrder);
    }

    @Override
    public CustomerOrderPaginateDto searchAll(String searchText, int page, int size) {
        return CustomerOrderPaginateDto.builder()
                .count(
                        customerOrderRepo.searchCount(searchText)
                )
                .dataList(
                        customerOrderRepo.searchAll(searchText, PageRequest.of(page, size))
                                .stream().map(this::toCustomerOrderResponseDto).collect(Collectors.toList())
                )
                .build();
    }

    private CustomerOrderResponseDto toCustomerOrderResponseDto(CustomerOrder customerOrder) {
        if (customerOrder == null) {
            return null;
        }
        return CustomerOrderResponseDto.builder()
                .orderId(customerOrder.getOrderId())
                .orderDate(customerOrder.getOrderDate())
                .userId(customerOrder.getUserId())
                .totalAmount(customerOrder.getTotalAmount())
                .orderDetails(
                        customerOrder.getProducts().stream().map(this::toOrderDetailResponseDto).collect(Collectors.toList())
                )
                .remark(customerOrder.getRemark())
                .status(customerOrder.getOrderStatus().getStatus())
                .build();
    }

    private OrderDetailResponseDto toOrderDetailResponseDto(OrderDetail orderDetail) {
        if (orderDetail == null) {
            return null;
        }
        return OrderDetailResponseDto.builder()
                .productId(orderDetail.getProductId())
                .detailId(orderDetail.getDetailId())
                .discount(orderDetail.getDiscount())
                .qty(orderDetail.getQty())
                .unitPrice(orderDetail.getUnitPrice())
                .build();
    }

    private OrderDetail createOrderDetail(OrderDetailRequestDto requestDto, CustomerOrder order) {
        if (requestDto == null) {
            return null;
        }
        return OrderDetail.builder()
                .detailId(UUID.randomUUID().toString())
                .unitPrice(requestDto.getUnitPrice())
                .discount(requestDto.getDiscount())
                .qty(requestDto.getQty())
                .customerOrder(order)
                .build();
    }
}
