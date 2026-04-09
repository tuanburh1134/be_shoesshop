package com.shoes.ecommerce.repository;

import com.shoes.ecommerce.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByUserId(Long userId);
    Optional<OrderEntity> findByPayosOrderCode(Long payosOrderCode);
}
