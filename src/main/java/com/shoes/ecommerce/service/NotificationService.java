package com.shoes.ecommerce.service;

import com.shoes.ecommerce.entity.Notification;
import com.shoes.ecommerce.entity.User;

import java.util.List;

public interface NotificationService {
    Notification createNotification(User user, String deviceId, String message);
    List<Notification> listByDeviceId(String deviceId);
}
