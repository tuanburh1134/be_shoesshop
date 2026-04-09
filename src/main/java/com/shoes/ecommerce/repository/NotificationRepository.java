package com.shoes.ecommerce.repository;

import com.shoes.ecommerce.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByDeviceIdOrderByCreatedAtDesc(String deviceId);
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
}
