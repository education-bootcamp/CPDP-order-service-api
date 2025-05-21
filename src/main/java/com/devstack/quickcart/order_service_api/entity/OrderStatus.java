package com.devstack.quickcart.order_service_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity(name = "order_status")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderStatus {
    @Id
    @Column(name="status_id", unique=true, nullable=false, length=80)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String statusId;
    @Column(name="status", nullable=false, length=80, unique = true) //PENDING, REJECTED
    private String status;
    //======================
    @OneToMany(mappedBy = "orderStatus")
    private Set<CustomerOrder> customerOrders = new HashSet<>();

}
