package com.devstack.quickcart.order_service_api.repo;

import com.devstack.quickcart.order_service_api.entity.CustomerOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CustomerOrderRepo extends JpaRepository<CustomerOrder, String> {

    @Query(nativeQuery = true, value = "SELECT * FROM customer_order WHERE remark LIKE %?1%")
    public Page<CustomerOrder> searchAll(String remark, Pageable pageable);

    @Query(nativeQuery = true, value = "SELECT COUNT(order_id) FROM customer_order WHERE remark LIKE %?1%")
    public long searchCount(String remark);

    @Query(nativeQuery = true, value = "SELECT * FROM customer_order WHERE intent_id=?1")
    public Optional<CustomerOrder> getContainingIntentId(String intentId);

}
