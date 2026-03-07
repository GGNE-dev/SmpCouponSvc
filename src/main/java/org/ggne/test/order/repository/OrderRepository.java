package org.ggne.test.order.repository;

import org.ggne.test.order.domain.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Orders, Long> {
    List<Orders> findAllByUserId(Long userId);
}
