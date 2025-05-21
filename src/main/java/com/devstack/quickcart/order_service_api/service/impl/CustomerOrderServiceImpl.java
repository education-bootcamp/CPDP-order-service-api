package com.devstack.quickcart.order_service_api.service.impl;

import com.devstack.quickcart.order_service_api.dto.request.CustomerOrderRequestDto;
import com.devstack.quickcart.order_service_api.dto.request.OrderDetailRequestDto;
import com.devstack.quickcart.order_service_api.dto.response.CustomerOrderResponseDto;
import com.devstack.quickcart.order_service_api.dto.response.OrderDetailResponseDto;
import com.devstack.quickcart.order_service_api.dto.response.paginate.CustomerOrderPaginateDto;
import com.devstack.quickcart.order_service_api.entity.CustomerOrder;
import com.devstack.quickcart.order_service_api.entity.OrderDetail;
import com.devstack.quickcart.order_service_api.entity.OrderStatus;
import com.devstack.quickcart.order_service_api.exception.EntryNotFoundException;
import com.devstack.quickcart.order_service_api.repo.CustomerOrderRepo;
import com.devstack.quickcart.order_service_api.repo.OrderDetailRepo;
import com.devstack.quickcart.order_service_api.repo.OrderStatusRepo;
import com.devstack.quickcart.order_service_api.service.CustomerOrderService;
import lombok.RequiredArgsConstructor;
import org.hibernate.query.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerOrderServiceImpl implements CustomerOrderService {

    private final CustomerOrderRepo customerOrderRepo;
    private final OrderStatusRepo orderStatusRepo;
    private final OrderDetailRepo orderDetailRepo;

    @Transactional
    @Override
    public void createOrder(CustomerOrderRequestDto requestDto) {
        // you must use a real UserId
        var userId = "TempUserId";
        OrderStatus orderStatus = orderStatusRepo.findByStatus("PENDING").orElseThrow(() -> new EntryNotFoundException("Order Status Not Found. so you can't place an order please contact admin"));

        var orderId = UUID.randomUUID().toString();

        CustomerOrder customerOrder = new CustomerOrder();
        customerOrder.setOrderId(orderId);
        customerOrder.setOrderDate(new Date());
        customerOrder.setRemark("");
        customerOrder.setTotalAmount(requestDto.getTotalAmount());
        customerOrder.setUserId(userId);
        customerOrder.setOrderStatus(orderStatus);
        customerOrderRepo.save(customerOrder);

        for (OrderDetailRequestDto t : requestDto.getOrderDetails()) {
            orderDetailRepo.save(OrderDetail.builder()
                    .qty(t.getQty())
                    .productId(t.getProductId())
                    .customerOrder(customerOrder)
                    .discount(t.getDiscount())
                    .unitPrice(t.getUnitPrice()).build());
        }

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
