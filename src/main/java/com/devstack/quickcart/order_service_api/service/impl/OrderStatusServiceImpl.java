package com.devstack.quickcart.order_service_api.service.impl;

import com.devstack.quickcart.order_service_api.entity.OrderStatus;
import com.devstack.quickcart.order_service_api.repo.OrderStatusRepo;
import com.devstack.quickcart.order_service_api.service.OrderStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class OrderStatusServiceImpl implements OrderStatusService {

    private final OrderStatusRepo orderStatusRepo;

    @Override
    public void initializeStatusList() {
        long count = orderStatusRepo.count();
        if (count == 0) {
            orderStatusRepo.saveAll(
                    List.of(
                            OrderStatus.builder()
                                    .status("PENDING")
                                    .build(),
                            OrderStatus.builder()
                                    .status("COMPLETED")
                                    .build(),
                            OrderStatus.builder()
                                    .status("REJECTED_BY_USER")
                                    .build(),
                            OrderStatus.builder()
                                    .status("REJECTED_BY_ADMIN")
                                    .build()
                    )
            );
        }
    }
}
