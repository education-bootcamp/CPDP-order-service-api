package com.devstack.quickcart.order_service_api;

import com.devstack.quickcart.order_service_api.service.OrderStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RequiredArgsConstructor
public class OrderServiceApiApplication implements CommandLineRunner {

	private final OrderStatusService orderStatusService;

	@Override
	public void run(String... args) throws Exception {
		initializeStatus();
	}

	public static void main(String[] args) {
		SpringApplication.run(OrderServiceApiApplication.class, args);
	}

	private void initializeStatus(){
		orderStatusService.initializeStatusList();
	}

}
